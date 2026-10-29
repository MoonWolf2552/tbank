package academy.backend.market_pulse.repository.store.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import academy.backend.market_pulse.repository.store.WatchlistStore;

public class WatchlistStoreImpl implements WatchlistStore {

    @Override
    public void save(Path path, Set<String> tickers) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, tickers, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить watchlist в " + path, e);
        }
    }

    @Override
    public Set<String> load(Path path) {
        if (!Files.exists(path)) {
            return new LinkedHashSet<>();
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            Set<String> tickers = new LinkedHashSet<>();

            for (String line : lines) {
                String ticker = line.strip();
                if (!ticker.isEmpty()) {
                    tickers.add(ticker);
                }
            }

            return tickers;
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать watchlist из " + path, e);
        }
    }
}