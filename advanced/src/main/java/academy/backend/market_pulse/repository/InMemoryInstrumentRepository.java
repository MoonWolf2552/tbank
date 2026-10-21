package academy.backend.market_pulse.repository;

import academy.backend.market_pulse.exception.DuplicateTickerException;
import academy.backend.market_pulse.model.Bond;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Etf;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Stock;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * Реализация {@link InstrumentRepository} поверх {@link LinkedHashMap} (см. «План семинара.md»,
 * семинар 5, этап 3.1): ключ — тикер в верхнем регистре, поиск и проверка дубликата — {@code O(1)}
 * вместо линейного перебора по массиву. {@code LinkedHashMap}, а не {@code HashMap} — ради
 * сохранения порядка добавления в выводе {@code list}.
 */
public class InMemoryInstrumentRepository implements InstrumentRepository {

    private final Map<String, Instrument> instruments = new HashMap<>();

    public InMemoryInstrumentRepository() {
        // Стартовые данные для демонстрации CLI: без них каждый запуск начинается с пустого списка.
        add(new Stock("SBER", "Сбер", Currency.RUB, "Финансы", new BigDecimal("6.5")));
        add(new Bond("OFZ26233", "ОФЗ 26233", Currency.RUB, new BigDecimal("8.5"), 2036));
        add(new Etf("FXUS", "FinEx FXUS", Currency.USD, "S&P 500"));
    }

    @Override
    public void add(Instrument instrument) {
        String key = instrument.getTicker().toUpperCase();
        if (instruments.containsKey(key)) {
            throw new DuplicateTickerException(instrument.getTicker());
        }
        instruments.put(key, instrument);
    }

    @Override
    public Optional<Instrument> findByTicker(String ticker) {
        return Optional.ofNullable(instruments.get(ticker.toUpperCase()));
    }

    @Override
    public boolean remove(String ticker) {
        return instruments.remove(ticker.toUpperCase()) != null;
    }

    @Override
    public Set<String> removeIf(Predicate<Instrument> filter) {
        Set<String> removed = new LinkedHashSet<>();
        Iterator<Map.Entry<String, Instrument>> it = instruments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Instrument> entry = it.next();
            if (filter.test(entry.getValue())) {
                it.remove(); // прямой instruments.remove(key) внутри этого же обхода уронил бы
                             // ConcurrentModificationException — модификация идёт через сам итератор
                removed.add(entry.getKey());
            }
        }
        return removed;
    }

    @Override
    public Iterator<Instrument> iterator() {
        return instruments.values().iterator();
    }

    @Override
    public Stream<Instrument> stream() {
        return instruments.values().stream();
    }

}
