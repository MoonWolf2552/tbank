package academy.backend.market_pulse.repository.store.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import academy.backend.market_pulse.model.Bond;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Etf;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.model.Stock;
import academy.backend.market_pulse.repository.store.QuoteFileStore;

public class QuoteFileStoreImpl implements QuoteFileStore {

    private static final String SEP = "\t";
    private static final int COLUMNS = 8;

    @Override
    public void save(Path path, List<Quote> quotes) {
        List<String> lines = new ArrayList<>(quotes.size());

        for (Quote quote : quotes) {
            lines.add(toLine(quote));
        }

        try {

            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить котировки в " + path, e);
        }
    }

    @Override
    public List<Quote> load(Path path) {
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<Quote> result = new ArrayList<>(lines.size());

            for (String line : lines) {
                if (!line.isBlank()) {
                    result.add(fromLine(line));
                }
            }

            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать котировки из " + path, e);
        }
    }

    private String toLine(Quote quote) {
        Instrument instrument = quote.getInstrument();
        String type = instrument.getType();
        String field1;
        String field2 = "";

        switch (type) {
            case "STOCK" -> {
                Stock stock = (Stock) instrument;
                field1 = stock.getSector();
                field2 = stock.getDividendYield().toPlainString();
            }
            case "BOND" -> {
                Bond bond = (Bond) instrument;
                field1 = bond.getCouponRate().toPlainString();
                field2 = Integer.toString(bond.getMaturityYear());
            }
            case "ETF" -> field1 = ((Etf) instrument).getTrackingIndex();
            default -> field1 = "";
        }

        return String.join(SEP,
                type,
                instrument.getTicker(),
                instrument.getName() == null ? "" : instrument.getName(),
                instrument.getCurrency().name(),
                quote.getPrice().toPlainString(),
                quote.getChangePercent().toPlainString(),
                field1 == null ? "" : field1,
                field2);
    }

    private Quote fromLine(String line) {
        String[] parts = line.split(SEP, -1);

        if (parts.length < COLUMNS) {
            throw new IllegalStateException("Некорректная строка котировки: " + line);
        }

        String type = parts[0];
        String ticker = parts[1];
        String name = parts[2];
        Currency currency = Currency.valueOf(parts[3]);
        BigDecimal price = new BigDecimal(parts[4]);
        BigDecimal change = new BigDecimal(parts[5]);
        String field1 = parts[6];
        String field2 = parts[7];

        Instrument instrument = switch (type) {
            case "STOCK" -> new Stock(ticker, name, currency, field1, new BigDecimal(field2));
            case "BOND" -> new Bond(ticker, name, currency, new BigDecimal(field1),
                    Integer.parseInt(field2));
            case "ETF" -> new Etf(ticker, name, currency, field1);
            default -> throw new IllegalStateException("Неизвестный тип инструмента: " + type);
        };

        return new Quote(instrument, price, change);
    }
}