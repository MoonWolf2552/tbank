package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import academy.backend.market_pulse.stat.StatCode;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Котировка с наибольшим изменением цены — {@code max(Comparator)}, {@code Optional<Quote>}. */
public class TopGainerStat implements Stat<Optional<Quote>> {

    @Override
    public StatCode getCode() {
        return StatCode.TOP_GAINER;
    }

    @Override
    public Optional<Quote> calculateStats(List<Quote> quotes) {
        return quotes.stream()
                .max(Comparator.comparing(Quote::getChangePercent));
    }
}
