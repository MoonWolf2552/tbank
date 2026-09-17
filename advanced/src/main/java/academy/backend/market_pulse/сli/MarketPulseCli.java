package academy.backend.market_pulse.сli;

import picocli.CommandLine.Command;


@Command(name = "market-pulse")
public class MarketPulseCli implements Runnable {

    @Override
    public void run() {
        System.out.println("Введите --help для получения списка доступных команд");
    }

}
