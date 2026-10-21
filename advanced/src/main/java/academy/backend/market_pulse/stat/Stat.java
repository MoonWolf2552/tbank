package academy.backend.market_pulse.stat;

import academy.backend.market_pulse.model.Quote;

import java.util.List;

/**
 * Одна статистика по списку котировок — команда {@code stats} (семинар 6).
 *
 * <p>Та же идея расширяемости, что у {@code InstrumentFilter} ({@code filter} package): новый вид
 * отбора добавлялся через новый класс, реализующий {@code InstrumentFilter}, без изменения
 * существующих — по сути, Chain of Responsibility/Command: каждая реализация инкапсулирует одну
 * операцию и подключается в одном месте ({@code Main}), а вызывающий код ({@code StatsCommand})
 * не знает заранее, сколько их и какие именно. Добавить новую статистику — написать новый класс,
 * реализующий {@link Stat}, и дописать экземпляр в список в {@code Main}; {@code StatsCommand} при
 * этом не меняется.
 *
 * <p>{@link #getCode()} подписывает статистику в выводе — ровно так, как {@code Operator} у
 * {@code PriceFilter} различает варианты одного семейства. {@link #calculateStats(List)} — метод с
 * одним фиксированным аргументом, как {@code matches(Instrument)} у фильтра: параметры конкретной
 * статистики (порог, окно и т.п.), если нужны, передаются через конструктор реализации, а не через
 * аргументы метода.
 *
 * @param <S> тип результата конкретной статистики — свой у каждой: число, доля, {@code Map},
 *            список, конкретная {@code Quote}
 */
public interface Stat<S> {

    StatCode getCode();

    S calculateStats(List<Quote> quotes);
}
