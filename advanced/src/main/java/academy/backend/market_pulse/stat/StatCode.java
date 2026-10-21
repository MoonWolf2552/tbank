package academy.backend.market_pulse.stat;

/**
 * Код статистики — подписывает результат конкретной {@link Stat} в выводе команды {@code stats}
 * (семинар 6), не завязываясь на {@code getClass().getSimpleName()}.
 */
public enum StatCode {
    TICKERS_BY_TYPE,
    COUNT_BY_CURRENCY,
    AVERAGE_CHANGE,
    TOTAL_PRICE,
    GAIN_LOSS_SPLIT,
    TOP_GAINER,
    NAIVE_MOVING_AVERAGE_PRICE,
    PARALLEL_AVERAGE_CHANGE,
    WINDOWED_AVERAGE_PRICE,
    MOVING_AVERAGE_PRICE
}
