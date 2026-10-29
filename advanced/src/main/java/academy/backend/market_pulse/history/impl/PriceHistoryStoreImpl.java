package academy.backend.market_pulse.history.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import academy.backend.market_pulse.history.PriceHistoryStore;
import academy.backend.market_pulse.history.PricePoint;

public class PriceHistoryStoreImpl implements PriceHistoryStore {

    private static final String SEP = "\t";
    private static final String EXT = ".txt";

    @Override
    public Path append(Path directory, PricePoint point) {
        try {
            Files.createDirectories(directory);

            Instant ts = point.timestamp();
            String base = point.ticker() + "-" + ts.getEpochSecond() + "-" + ts.getNano();
            Path file = directory.resolve(base + EXT);

            int suffix = 0;

            while (Files.exists(file)) {
                suffix++;
                file = directory.resolve(base + "-" + suffix + EXT);
            }

            String line = String.join(SEP,
                    point.ticker(),
                    Long.toString(ts.getEpochSecond()),
                    Integer.toString(ts.getNano()),
                    point.price().toPlainString());

            Files.writeString(file, line, StandardCharsets.UTF_8);

            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось дописать точку истории в " + directory, e);
        }
    }

    @Override
    public List<PricePoint> load(Path directory, String ticker) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        String prefix = ticker + "-";
        List<PricePoint> points = new ArrayList<>();

        try (Stream<Path> files = Files.list(directory)) {
            List<Path> matching = files
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(EXT);
                    })
                    .toList();

            for (Path file : matching) {
                String line = Files.readString(file, StandardCharsets.UTF_8).strip();
                if (!line.isEmpty()) {
                    points.add(parse(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать историю из " + directory, e);
        }

        points.sort(Comparator.comparing(PricePoint::timestamp));

        return points;
    }

    private PricePoint parse(String line) {
        String[] parts = line.split(SEP, -1);

        if (parts.length < 4) {
            throw new IllegalStateException("Некорректная строка истории: " + line);
        }

        String ticker = parts[0];
        long epochSecond = Long.parseLong(parts[1]);
        int nano = Integer.parseInt(parts[2]);
        BigDecimal price = new BigDecimal(parts[3]);

        return new PricePoint(ticker, Instant.ofEpochSecond(epochSecond, nano), price);
    }
}