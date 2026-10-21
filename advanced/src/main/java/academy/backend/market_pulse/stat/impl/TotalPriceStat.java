package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import academy.backend.market_pulse.stat.StatCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Суммарная цена всех котировок — через {@code reduce} над {@code BigDecimal}, а не
 * {@code mapToDouble().sum()}: это деньги, они не должны терять точность округлением в
 * {@code double} (см. {@link AverageChangeStat}, где такая потеря уже допустима).
 */
public class TotalPriceStat implements Stat<BigDecimal> {

    @Override
    public StatCode getCode() {
        return StatCode.TOTAL_PRICE;
    }

    @Override
    public BigDecimal calculateStats(List<Quote> quotes) {
        return quotes.stream()
                .map(Quote::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
