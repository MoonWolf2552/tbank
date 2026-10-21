package academy.backend.market_pulse.exercise;

import academy.backend.market_pulse.model.Bond;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Etf;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.model.Stock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Контракт для упражнений {@link StreamExercises} (см. «План семинара.md», семинар 6, раздел «Что уже реализовано»).
 * Наследники подставляют свою реализацию в {@link #createExercises()}: {@code ...ReferenceTest} —
 * сверка эталона (должен быть зелёным всегда), {@code ...SkeletonTest} — прогресс студента
 * (становится зелёным по мере реализации {@link StreamExercisesSkeleton}).
 *
 * <p>Один и тот же набор из 10 котировок на все упражнения — {@link #sampleQuotes()}. Намеренно
 * содержит: дубликат тикера {@code SBER} с разными ценами (упражнения 11–12), и серию из четырёх
 * подряд идущих убыточных котировок {@code GAZP → LKOH → OFZ26233 → MGNT}, за которой позже следует
 * более короткая серия из двух ({@code MTSS → VTBR}) — упражнение 16 должно вернуть именно 4, а не
 * длину последней серии или сумму серий.
 */
public abstract class StreamExercisesContractTest {

    protected abstract StreamExercises createExercises();

    private static List<Quote> sampleQuotes() {
        Instrument sber = new Stock("SBER", "Сбер", Currency.RUB, "Финансы", new BigDecimal("6.5"));
        Instrument gazp = new Stock("GAZP", "Газпром", Currency.RUB, "Энергетика", new BigDecimal("8.0"));
        Instrument lkoh = new Stock("LKOH", "Лукойл", Currency.RUB, "Энергетика", new BigDecimal("9.0"));
        Instrument ofz = new Bond("OFZ26233", "ОФЗ 26233", Currency.RUB, new BigDecimal("8.5"), 2036);
        Instrument mgnt = new Stock("MGNT", "Магнит", Currency.RUB, "Ритейл", new BigDecimal("5.0"));
        Instrument aapl = new Stock("AAPL", "Apple", Currency.USD, "Технологии", new BigDecimal("0.5"));
        Instrument fxus = new Etf("FXUS", "FinEx FXUS", Currency.USD, "S&P 500");
        Instrument mtss = new Stock("MTSS", "МТС", Currency.RUB, "Телеком", new BigDecimal("7.0"));
        Instrument vtbr = new Stock("VTBR", "ВТБ", Currency.RUB, "Финансы", new BigDecimal("4.0"));

        return List.of(
                new Quote(sber, new BigDecimal("250.00"), new BigDecimal("1.20")),
                new Quote(gazp, new BigDecimal("120.50"), new BigDecimal("-0.80")),
                new Quote(lkoh, new BigDecimal("7100.00"), new BigDecimal("-1.50")),
                new Quote(ofz, new BigDecimal("780.00"), new BigDecimal("-0.15")),
                new Quote(mgnt, new BigDecimal("5400.00"), new BigDecimal("-0.60")),
                new Quote(aapl, new BigDecimal("225.30"), new BigDecimal("0.50")),
                new Quote(sber, new BigDecimal("255.00"), new BigDecimal("0.80")),
                new Quote(fxus, new BigDecimal("6.85"), new BigDecimal("0.90")),
                new Quote(mtss, new BigDecimal("300.00"), new BigDecimal("-0.20")),
                new Quote(vtbr, new BigDecimal("95.00"), new BigDecimal("-0.10")));
    }

    @Test
    void tickersInCurrency_returnsOnlyMatchingCurrencyInOrder() {
        List<String> usdTickers = createExercises().tickersInCurrency(sampleQuotes(), Currency.USD);

        assertEquals(List.of("AAPL", "FXUS"), usdTickers);
    }

    @Test
    void topByPrice_returnsNHighestPricesDescending() {
        List<Quote> top2 = createExercises().topByPrice(sampleQuotes(), 2);

        assertEquals(List.of("LKOH", "MGNT"), top2.stream().map(q -> q.getInstrument().getTicker()).toList());
    }

    @Test
    void distinctCurrencies_hasNoDuplicates() {
        Set<Currency> currencies = createExercises().distinctCurrencies(sampleQuotes());

        assertEquals(Set.of(Currency.RUB, Currency.USD), currencies);
    }

    @Test
    void hasAnyLoser_trueWhenAtLeastOneNegativeChange() {
        assertTrue(createExercises().hasAnyLoser(sampleQuotes()));
        assertFalse(createExercises().hasAnyLoser(List.of(sampleQuotes().get(0))));
    }

    @Test
    void countByType_countsOnlyMatchingType() {
        assertEquals(1, createExercises().countByType(sampleQuotes(), "BOND"));
        assertEquals(1, createExercises().countByType(sampleQuotes(), "ETF"));
        assertEquals(8, createExercises().countByType(sampleQuotes(), "STOCK"));
    }

    @Test
    void totalPrice_sumsAllPricesExactly() {
        BigDecimal total = createExercises().totalPrice(sampleQuotes());

        assertEquals(0, new BigDecimal("14532.65").compareTo(total));
    }

    @Test
    void averageChangePercent_averagesAllChanges() {
        double average = createExercises().averageChangePercent(sampleQuotes());

        assertEquals(0.005, average, 1e-9);
    }

    @Test
    void countByTypeGrouped_groupsByInstrumentType() {
        Map<String, Long> byType = createExercises().countByTypeGrouped(sampleQuotes());

        assertEquals(Map.of("STOCK", 8L, "BOND", 1L, "ETF", 1L), byType);
    }

    @Test
    void averageChangeByType_perGroupAverageNotOverallAverage() {
        Map<String, Double> averages = createExercises().averageChangeByType(sampleQuotes());

        assertEquals(-0.15, averages.get("BOND"), 1e-9);
        assertEquals(0.9, averages.get("ETF"), 1e-9);
    }

    @Test
    void partitionByGain_exactlyTwoKeysTrueAndFalse() {
        Map<Boolean, List<Quote>> partitioned = createExercises().partitionByGain(sampleQuotes());

        assertEquals(4, partitioned.get(true).size());
        assertEquals(6, partitioned.get(false).size());
    }

    @Test
    void latestByTicker_keepsHigherPriceOnDuplicateTicker() {
        Map<String, Quote> byTicker = createExercises().latestByTicker(sampleQuotes());

        assertEquals(0, new BigDecimal("255.00").compareTo(byTicker.get("SBER").getPrice()));
        assertEquals(9, byTicker.size());
    }

    @Test
    void joinTickers_joinsAllInOrderWithoutDeduping() {
        String joined = createExercises().joinTickers(sampleQuotes());

        assertEquals("[SBER, GAZP, LKOH, OFZ26233, MGNT, AAPL, SBER, FXUS, MTSS, VTBR]", joined);
    }

    @Test
    void tickersJoinedByCurrency_groupsThenJoinsPerGroup() {
        Map<Currency, String> byCurrency = createExercises().tickersJoinedByCurrency(sampleQuotes());

        assertEquals("SBER, GAZP, LKOH, OFZ26233, MGNT, SBER, MTSS, VTBR", byCurrency.get(Currency.RUB));
        assertEquals("AAPL, FXUS", byCurrency.get(Currency.USD));
    }

    @Test
    void findByTickers_preservesRequestedOrderAndSkipsUnknown() {
        List<Quote> found = createExercises()
                .findByTickers(sampleQuotes(), List.of("VTBR", "UNKNOWN", "AAPL"));

        assertEquals(List.of("VTBR", "AAPL"), found.stream().map(q -> q.getInstrument().getTicker()).toList());
    }

    @Test
    void movingAveragePrice_averagesConsecutiveWindows() {
        List<Quote> threeQuotes = sampleQuotes().subList(0, 3); // SBER 250, GAZP 120.50, LKOH 7100

        List<BigDecimal> sma = createExercises().movingAveragePrice(threeQuotes, 2);

        assertEquals(List.of(new BigDecimal("185.25"), new BigDecimal("3610.25")), sma);
    }

    @Test
    void longestLossStreak_findsLongestRunNotLastOrTotal() {
        int longest = createExercises().longestLossStreak(sampleQuotes());

        assertEquals(4, longest);
    }

    @Test
    void longestLossStreak_zeroWhenNoLosses() {
        List<Quote> onlyGainers = sampleQuotes().stream()
                .filter(q -> q.getChangePercent().signum() >= 0)
                .toList();

        assertEquals(0, createExercises().longestLossStreak(onlyGainers));
    }
}
