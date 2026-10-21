package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import academy.backend.market_pulse.stat.StatCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Для каждого типа инструмента — строка со всеми его тикерами через {@code ", "}. Composite
 * {@code collect}: {@code groupingBy(classifier, downstream)}, у которого downstream сам составной
 * — {@code mapping(valueExtractor, joining(delimiter))}. {@code mapping} сначала проецирует элемент
 * группы (котировку) в тикер, и только потом {@code joining} склеивает тикеры внутри группы; без
 * {@code mapping} пришлось бы отдать {@code joining} прямо на {@code Quote}, а {@code Quote} — не
 * {@code CharSequence}, так не скомпилируется.
 */
public class TickersByTypeStat implements Stat<Map<String, String>> {

    @Override
    public StatCode getCode() {
        return StatCode.TICKERS_BY_TYPE;
    }

    @Override
    public Map<String, String> calculateStats(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.groupingBy(
                        quote -> quote.getInstrument().getType(),
                        Collectors.mapping(
                                quote -> quote.getInstrument().getTicker(),
                                Collectors.joining(", "))));
    }
}
