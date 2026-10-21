package academy.backend.market_pulse.repository;

import academy.backend.market_pulse.model.Instrument;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Хранилище инструментов. Выделено в интерфейс (в отличие от базового трека) — нужен для
 * JDK Dynamic Proxy и CGLIB, которым для работы требуется тип, отдельный от конкретной
 * реализации.
 */
public interface InstrumentRepository extends Iterable<Instrument> {

    void add(Instrument instrument);

    /**
     * {@code Optional} вместо {@code null}: отсутствие значения заявлено в сигнатуре
     */
    Optional<Instrument> findByTicker(String ticker);

    /**
     * Удаляет инструмент по тикеру (делистинг). {@code true}, если инструмент был и его удалили;
     * {@code false}, если такого тикера не было. Точечное удаление по ключу — {@code Iterator} тут
     * не нужен (см. «План семинара.md», семинар 5, этап 5).
     */
    boolean remove(String ticker);

    /**
     * Массовый делистинг: удаляет все инструменты, подходящие под {@code filter} (например, все в
     * заданной валюте), и возвращает тикеры удалённых. В отличие от {@link #remove(String)} —
     * реализация обязана пройтись по хранилищу и решить по каждому элементу, удалять его или нет,
     * а значит без {@code Iterator.remove()} (или {@code removeIf} поверх него) не обойтись (см.
     * «План семинара.md», семинар 5, этап 5).
     */
    Set<String> removeIf(Predicate<Instrument> filter);

    /**
     * Потоковый доступ к инструментам — вход в Stream API для декларативной фильтрации, сортировки
     * и агрегации (см. «План семинара.md», семинар 6, раздел «Что уже реализовано»).
     */
    Stream<Instrument> stream();

}
