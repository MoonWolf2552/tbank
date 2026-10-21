package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import academy.backend.market_pulse.stat.StatCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сколько котировок выросло ({@code changePercent > 0}), а сколько — нет: ровно два ключа,
 * {@code true}/{@code false}. {@code partitioningBy} со вторым коллектором ({@code counting}) —
 * как {@code groupingBy} в {@link CountByTypeStat}, но классификатор здесь булев.
 */
public class GainLossSplitStat implements Stat<Map<Boolean, Long>> {

    @Override
    public StatCode getCode() {
        return StatCode.GAIN_LOSS_SPLIT;
    }

    @Override
    public Map<Boolean, Long> calculateStats(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.partitioningBy(
                        quote -> quote.getChangePercent().signum() > 0,
                        Collectors.counting()));
    }
}
