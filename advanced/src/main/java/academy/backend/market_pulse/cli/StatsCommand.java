package academy.backend.market_pulse.cli;

import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.service.QuoteSource;
import academy.backend.market_pulse.stat.Stat;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;


/**
 * Сводка по котировкам: печатает результат каждой зарегистрированной {@link Stat} (семинар 6,
 * этап «Статистика как расширяемость»). Сама команда не знает, сколько статистик и какие именно —
 * список собирается в {@code Main} (см. {@code Main.stats()}), а новая статистика добавляется без
 * изменения этого класса: пишется новый класс в {@code stat.impl} и дописывается в тот список —
 * ровно так же, как новый {@code InstrumentFilter} добавлялся без изменения {@code ListCommand}.
 */
@Command(name = "stats", description = "Сводка по котировкам: печатает все зарегистрированные статистики")
public class StatsCommand implements Callable<Integer> {

    private final QuoteSource source;
    private final List<Stat<?>> stats;

    public StatsCommand(QuoteSource source, List<Stat<?>> stats) {
        this.source = source;
        this.stats = stats;
    }

    @Override
    public Integer call() {
        List<Quote> quotes = List.copyOf(source.all());
        for (Stat<?> stat : stats) {
            System.out.println(stat.getCode() + ": " + stat.calculateStats(quotes));
        }
        return 0;
    }
}
