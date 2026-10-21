package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.gather.MovingAverageGatherer;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import academy.backend.market_pulse.stat.StatCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Переписанный {@link NaiveMovingAveragePriceStat} (этап 3): та же формула — скользящее среднее
 * цены по окнам из {@value #WINDOW_SIZE} — но через кастомный {@link MovingAverageGatherer} (тот
 * же класс, что использует команда {@code history TICKER --sma K}). Окно и сумма — состояние
 * {@code integrator}, среднее считается инкрементально по ходу одного прохода ({@code O(N)}), без
 * {@code subList} и без пересчёта заново на каждое окно — см. {@link NaiveMovingAveragePriceStat}
 * для версии «в лоб» на базовых операциях и {@link WindowedAveragePriceStat} — промежуточную
 * версию на встроенном {@code Gatherers.windowSliding}, которая тоже избегает ручных индексов, но
 * материализует окна-списки.
 */
public class MovingAveragePriceStat implements Stat<List<BigDecimal>> {

    private static final int WINDOW_SIZE = 3;

    @Override
    public StatCode getCode() {
        return StatCode.MOVING_AVERAGE_PRICE;
    }

    @Override
    public List<BigDecimal> calculateStats(List<Quote> quotes) {
        return quotes.stream()
                .map(Quote::getPrice)
                .gather(MovingAverageGatherer.movingAverage(WINDOW_SIZE))
                .toList();
    }
}
