# Stream API

---

## Что такое стрим

Stream — конвейер обработки последовательности элементов, а не структура данных. Он не хранит данные и не изменяет
источник (коллекцию, из которой создан): он описывает набор операций над элементами. Stream API позволяет выразить
обработку декларативно — *что* нужно получить, а не *как* пошагово это накапливать.

---

## От коллекции к стриму: stream()

```java
void example(List<Instrument> instruments) {
    Stream<Instrument> stream = instruments.stream();
}
```

Из массива — `Arrays.stream(array)`, из отдельных значений — `Stream.of(...)`, генераторы — `Stream.iterate`/`generate`.
Обратно в коллекцию возвращает терминальная операция (`toList()`, `collect(...)`).

Источник стрима — `Spliterator` (не `Iterator`): он умеет не только перебирать, но и сообщать размер и порядок и
делиться на части. Эта способность делиться лежит в основе параллельных стримов.

---

## Промежуточные и терминальные операции

- **Промежуточные** возвращают новый стрим и соединяются в цепочку: `filter`, `map`, `sorted`, `limit`, `distinct`.
- **Терминальные** запускают конвейер и дают результат или побочный эффект: `forEach`, `toList`, `count`, `reduce`,
  `collect`.

Стрим завершается ровно одной терминальной операцией и одноразов: повторный вызов терминальной операции на нём бросает
`IllegalStateException`.

```java
void example(List<Instrument> instruments) {
    List<String> tickers = instruments.stream()
            .filter(i -> i.getCurrency() == Currency.RUB)
            .map(Instrument::getTicker)
            .sorted()
            .toList();
}
```

### Базовые промежуточные операции — шпаргалка

| Операция | Что делает | Пример на каталоге инструментов |
|---|---|---|
| `filter(Predicate)` | оставляет элементы, прошедшие условие | `i -> i.getCurrency() == Currency.RUB` |
| `map(Function)` | преобразует каждый элемент в другой тип | `Instrument::getTicker` |
| `sorted()` / `sorted(Comparator)` | сортирует (natural order или по компаратору) | `Comparator.comparing(Quote::getChangePercent)` |
| `distinct()` | убирает дубликаты по `equals`/`hashCode` | `.map(Instrument::getCurrency).distinct()` |
| `limit(n)` / `skip(n)` | обрезает поток сверху / пропускает первые `n` | `movers --top N` → `.limit(N)` |
| `peek(Consumer)` | подсматривает элемент, не меняя поток | только для отладки — см. «Оговорки» |
| `takeWhile(Predicate)` / `dropWhile(Predicate)` | берёт/пропускает элементы, пока условие верно (на **отсортированном** источнике эквивалент «пока растёт») | `sorted.stream().takeWhile(q -> q.getChangePercent().signum() > 0)` — все растущие в начале отсортированного списка |

```java
void example(List<Quote> quotes) {
    // distinct по производному ключу: валюты, представленные в каталоге
    List<Currency> currencies = quotes.stream()
            .map(quote -> quote.getInstrument().getCurrency())
            .distinct()
            .toList();

    // takeWhile на предварительно отсортированном по убыванию доходности стриме:
    // "все растущие бумаги, пока не началось падение"
    List<Quote> gainersPrefix = quotes.stream()
            .sorted(Comparator.comparing(Quote::getChangePercent).reversed())
            .takeWhile(q -> q.getChangePercent().signum() > 0)
            .toList();
}
```

> **Типичная ошибка — `peek` как замена `map`/`forEach`.** `peek` задуман для отладки конвейера (залогировать элемент
> между операциями), а не для мутации состояния — JDK не гарантирует, что `peek` вообще выполнится для каждого
> элемента: если следующая операция короткозамкнутая (`limit`, `findFirst`), часть элементов до `peek` не дойдёт. Если
> нужен побочный эффект на каждый элемент — это `forEach` (терминальная операция), а не `peek`.

---

## Ленивость

Промежуточные операции ленивы: пока не вызвана терминальная, не обрабатывается ни один элемент, а элементы «протекают»
через конвейер по одному без промежуточных коллекций. Короткозамкнутые операции (`limit`, `findFirst`, `anyMatch`)
останавливают обработку, как только результат определён.

---

## reduce — свёртка без коллектора

`reduce` сворачивает стрим в одно значение через ассоциативную бинарную операцию. Три перегрузки:

```java
void example(List<Quote> quotes) {
    // 1 аргумент: Optional<T> — результата может не быть (пустой стрим)
    Optional<Quote> anyLoser = quotes.stream()
            .reduce((a, b) -> a.getChangePercent().signum() < 0 ? a : b);

    // 2 аргумента: identity + накопитель — результат гарантирован, но тип результата = тип элемента
    BigDecimal totalPrice = quotes.stream()
            .map(Quote::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 3 аргумента: identity + накопитель (T,U)->U + комбайнер (U,U)->U — тип результата другой;
    // комбайнер нужен для параллельных стримов (слить частичные результаты из разных потоков)
    BigDecimal totalChange = quotes.stream()
            .reduce(BigDecimal.ZERO,
                    (sum, quote) -> sum.add(quote.getChangePercent()),
                    BigDecimal::add);
}
```

> **Для семинариста:** `StatsCommand.java#L50-L52` (см. «Исходный код») — ровно двухаргументный `reduce` на `BigDecimal`:
> деньги через `reduce`, а не `mapToDouble().sum()`, чтобы не терять точность округлением в `double`. Подчеркнуть, что
> и `collect`, и `reduce` — обе свёртки; разница — в изменяемости аккумулятора (ниже).

---

## collect и Collectors

`reduce` подходит, когда накопитель — неизменяемое значение (`BigDecimal`, `int`). Когда накопитель — изменяемая
структура (список, `Map`, `StringBuilder`), `reduce` неудобен и небезопасен в параллельном режиме; для этого есть
`collect(Collector)` — обобщённый механизм **изменяемой** свёртки через тройку «поставщик контейнера / накопление
элемента / слияние контейнеров» (тот же принцип, что четвёрка `Gatherer`, но для терминальной операции, а не
промежуточной — см. «Gatherer API»). `java.util.stream.Collectors` — фабрика готовых коллекторов:

| Коллектор | Результат |
|---|---|
| `toList()` / `toSet()` | `List<T>` / `Set<T>` |
| `toMap(keyFn, valueFn)` | `Map<K,V>`; падает `IllegalStateException` при дубликате ключа |
| `toMap(keyFn, valueFn, mergeFn)` | то же, но дубликат ключа разрешается через `mergeFn` |
| `joining(delimiter, prefix, suffix)` | конкатенация строк с разделителем и обрамлением |
| `groupingBy(classifier)` | `Map<K, List<T>>` |
| `groupingBy(classifier, downstream)` | `Map<K, R>` — группа дополнительно схлопывается вторым коллектором |
| `mapping(mapper, downstream)` | коллектор-адаптер: сначала проецирует элемент через `mapper`, потом передаёт в `downstream` — обычно используется как `downstream` у `groupingBy` |
| `partitioningBy(predicate)` | `Map<Boolean, List<T>>` — всегда ровно два ключа, `true`/`false` |
| `summarizingDouble` / `summarizingInt` | один проход — сразу `count`/`sum`/`min`/`max`/`average` |
| `teeing(c1, c2, merger)` | два коллектора за один проход по стриму, результаты сливаются `merger`-ом |

```java
void example(List<Quote> quotes) {
    // groupingBy + downstream-коллектор: распределение по типу (как в StatsCommand),
    // но сразу со средней доходностью внутри группы, а не только счётчиком
    Map<String, Double> avgChangeByType = quotes.stream()
            .collect(Collectors.groupingBy(
                    q -> q.getInstrument().getClass().getSimpleName(),
                    Collectors.averagingDouble(q -> q.getChangePercent().doubleValue())));

    // toMap с merge-функцией: тикер -> котировка, на случай дубликатов тикера оставляем более дорогую
    Map<String, Quote> byTicker = quotes.stream()
            .collect(Collectors.toMap(
                    q -> q.getInstrument().getTicker(),
                    q -> q,
                    (existing, incoming) -> existing.getPrice().compareTo(incoming.getPrice()) >= 0 ? existing : incoming));

    // partitioningBy: ровно два списка — растущие и падающие/нулевые, без группировки по произвольному ключу
    Map<Boolean, List<Quote>> gainersVsRest = quotes.stream()
            .collect(Collectors.partitioningBy(q -> q.getChangePercent().signum() > 0));

    // summarizingDouble: count/sum/min/max/average за один проход, без пяти отдельных стримов
    DoubleSummaryStatistics changeStats = quotes.stream()
            .collect(Collectors.summarizingDouble(q -> q.getChangePercent().doubleValue()));

    // teeing (Java 12+): два агрегата за один проход по стриму вместо двух отдельных .stream()
    record MinMax(Optional<Quote> min, Optional<Quote> max) {}
    MinMax minMax = quotes.stream()
            .collect(Collectors.teeing(
                    Collectors.minBy(Comparator.comparing(Quote::getChangePercent)),
                    Collectors.maxBy(Comparator.comparing(Quote::getChangePercent)),
                    MinMax::new));

    // joining: человекочитаемый отчёт без ручной конкатенации в цикле
    String tickerList = quotes.stream()
            .map(q -> q.getInstrument().getTicker())
            .collect(Collectors.joining(", ", "[", "]"));

    // mapping как downstream у groupingBy: для каждой валюты — строка её тикеров.
    // Без mapping пришлось бы отдать joining прямо на Quote, а Quote — не CharSequence
    Map<Currency, String> tickersByCurrency = quotes.stream()
            .collect(Collectors.groupingBy(
                    q -> q.getInstrument().getCurrency(),
                    Collectors.mapping(q -> q.getInstrument().getTicker(), Collectors.joining(", "))));
}
```

> **Акцент — groupingBy с downstream vs два отдельных стрима.** `StatsCommand` считает `byType` и `avgChange` двумя
> независимыми конвейерами (см. этап 2.1) — это осознанно просто для показа «разные терминальные операции для разных
> агрегатов». `groupingBy(classifier, averagingDouble(...))` выше — тот же результат точнее (средняя доходность *по
> каждому типу*, а не общая) и за один проход. Хороший вопрос группе: почему `StatsCommand` всё равно не объединяет всё
> в один `teeing`/`groupingBy`-конвейер? Потому что независимые by-type/by-currency/avg/total агрегаты читаются яснее
> порознь — `teeing` оправдан, когда ровно два результата и они совпадают по входному стриму логически, а не просто
> «можно слепить один вызов».

> **Вопрос группе.** Чем `reduce(BigDecimal.ZERO, BigDecimal::add)` принципиально отличается от
> `collect(() -> new ArrayList<>(), List::add, List::addAll)`? (Подсказка: может ли накопитель быть **тем же самым**
> объектом на каждом шаге, или каждый раз создаётся новый — и что это значит для параллельного стрима.)

---

## flatMap и Optional.stream()

`flatMap` разворачивает «стрим стримов» в один стрим. Частая идиома — отбросить отсутствующие значения без проверок на
`null`: `Optional.stream()` даёт пустой стрим для `Optional.empty()` и одноэлементный — для присутствующего значения.

```java
void example(List<String> tickers, QuoteService quoteService) {
    List<Quote> found = tickers.stream()
            .map(quoteService::quoteFor)   // String -> Optional<Quote>
            .flatMap(Optional::stream)
            .toList();
}
```

---

## Короткозамкнутые терминальные операции

`anyMatch`/`allMatch`/`noneMatch`/`findFirst`/`findAny` не обходят весь стрим — останавливаются, как только ответ уже
известен:

```java
void example(List<Quote> quotes) {
    boolean hasLoser = quotes.stream()
            .anyMatch(q -> q.getChangePercent().signum() < 0);   // остановится на первом же падении

    boolean allInRub = quotes.stream()
            .allMatch(q -> q.getInstrument().getCurrency() == Currency.RUB);
}
```

> **Акцент.** `allMatch` на **пустом** стриме возвращает `true` (vacuous truth — условие не нарушено, потому что
> нарушать было нечему), `anyMatch` на пустом — `false`. Частый источник «семантически верного, но неожиданного»
> результата, если забыли проверить стрим на пустоту до агрегации.

---

## Оговорки

- Стрим не меняет источник: `list.stream().sorted()` не сортирует `list`.
- `mapToInt`/`mapToDouble` дают числовые стримы с `sum()`/`average()`, но перевод `BigDecimal` в `double` теряет
  точность — для денежных сумм остаются `BigDecimal` и `reduce`.
- Промежуточные операции должны быть чистыми (без побочных эффектов) — это условие корректной параллельной обработки:
  `reduce`/`collect` с изменяемым внешним состоянием (счётчик, `ArrayList` снаружи лямбды) на `parallelStream()` даёт
  гонку — несколько потоков пишут в один и тот же объект без синхронизации. На последовательном стриме такой код может
  случайно работать (как в `CompareCommand`, где `quoteFor` пишет в кеш `QuoteService` — см. «План семинара.md», этап
  2.2), но это повод для явной оговорки, а не образец для копирования.
- Стрим одноразов: `Stream<T> s = ...; s.count(); s.toList();` — второй вызов терминальной операции бросает
  `IllegalStateException: stream has already been operated upon or closed`. Если нужен результат дважды — считать
  один раз в коллекцию/переменную и переиспользовать её, а не стрим.
