package academy.backend.market_pulse.demo;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.repository.InMemoryInstrumentRepository;
import academy.backend.market_pulse.repository.InstrumentRepository;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

/**
 * Скрывает от клиента, как именно устроена прокси-обёртка над {@link InstrumentRepository},
 * которая замеряет время каждого вызова и логирует его. Клиент указывает только {@link Kind} —
 * какой из трёх механизмов использовать; про {@code InvocationHandler}, {@code Enhancer} и
 * ручное оборачивание каждого метода знает фабрика, а не он.
 */
public final class ProxyFactory {

    public enum Kind {
        STATIC, JDK_DYNAMIC, CGLIB
    }

    private ProxyFactory() {
    }

    public static InstrumentRepository timingRepository(Kind kind) {
        return switch (kind) {
            case STATIC -> new TimingInstrumentRepository(new InMemoryInstrumentRepository());
            case JDK_DYNAMIC -> jdkDynamicProxy(new InMemoryInstrumentRepository());
            case CGLIB -> cglibProxy();
        };
    }

    private static InstrumentRepository jdkDynamicProxy(InstrumentRepository target) {
        return (InstrumentRepository) Proxy.newProxyInstance(
                InstrumentRepository.class.getClassLoader(),
                new Class<?>[]{InstrumentRepository.class},
                (Object proxy, Method method, Object[] args) -> {
                    long start = System.nanoTime();
                    Object result = method.invoke(target, args);
                    System.out.printf("%s() выполнен за %d нс%n", method.getName(), System.nanoTime() - start);
                    return result;
                });
    }

    /**
     * JDK Dynamic Proxy, кеширующий {@code findByTicker} в {@link LruCache} — {@link LinkedHashMap}
     * в режиме {@code accessOrder=true} (порядок по последнему обращению, не по вставке) с
     * переопределённым {@code removeEldestEntry}, вытесняющим самый давно не запрашиваемый тикер
     * при превышении {@code cacheSize}. {@code add} грубо инвалидирует весь кеш целиком — искать
     * точечно, что именно устарело, сегодня не задача.
     */
    public static InstrumentRepository cachingRepository(InstrumentRepository target, int cacheSize) {
        Map<String, Optional<Instrument>> cache = new LruCache<>(cacheSize);
        return (InstrumentRepository) Proxy.newProxyInstance(
                InstrumentRepository.class.getClassLoader(),
                new Class<?>[]{InstrumentRepository.class},
                (Object proxy, Method method, Object[] args) -> {
                    if ("findByTicker".equals(method.getName())) {
                        String ticker = ((String) args[0]).toUpperCase();
                        if (cache.containsKey(ticker)) {
                            System.out.println("Cache hit: " + ticker);
                            return cache.get(ticker);
                        }
                        System.out.println("Cache miss: " + ticker);
                        Optional<Instrument> result = target.findByTicker(ticker);
                        cache.put(ticker, result);
                        return result;
                    }
                    if ("add".equals(method.getName())) {
                        cache.clear();
                    }
                    return method.invoke(target, args);
                });
    }

    /** {@code accessOrder=true} + {@code removeEldestEntry} — весь механизм LRU в двух строчках. */
    private static final class LruCache<K, V> extends LinkedHashMap<K, V> {

        private final int maxSize;

        private LruCache(int maxSize) {
            super(maxSize, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

    private static InstrumentRepository cglibProxy() {
        // CGLIB проксирует класс, а не готовый экземпляр: сгенерированный подкласс сам и есть
        // хранилище, поэтому в отличие от двух других вариантов здесь нет отдельного target.
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(InMemoryInstrumentRepository.class);
        enhancer.setCallback((MethodInterceptor) (obj, method, args, methodProxy) -> {
            long start = System.nanoTime();
            Object result = methodProxy.invokeSuper(obj, args);
            System.out.printf("%s() выполнен за %d нс%n", method.getName(), System.nanoTime() - start);
            return result;
        });
        return (InstrumentRepository) enhancer.create();
    }

    /** Самописный (статический) прокси — каждый метод интерфейса оборачивается вручную. */
    private static final class TimingInstrumentRepository implements InstrumentRepository {

        private final InstrumentRepository target;

        private TimingInstrumentRepository(InstrumentRepository target) {
            this.target = target;
        }

        @Override
        public void add(Instrument instrument) {
            long start = System.nanoTime();
            target.add(instrument);
            System.out.printf("add() выполнен за %d нс%n", System.nanoTime() - start);
        }

        @Override
        public Optional<Instrument> findByTicker(String ticker) {
            long start = System.nanoTime();
            Optional<Instrument> result = target.findByTicker(ticker);
            System.out.printf("findByTicker() выполнен за %d нс%n", System.nanoTime() - start);
            return result;
        }

        @Override
        public boolean remove(String ticker) {
            long start = System.nanoTime();
            boolean result = target.remove(ticker);
            System.out.printf("remove() выполнен за %d нс%n", System.nanoTime() - start);
            return result;
        }

        @Override
        public Set<String> removeIf(Predicate<Instrument> filter) {
            long start = System.nanoTime();
            Set<String> result = target.removeIf(filter);
            System.out.printf("removeIf() выполнен за %d нс%n", System.nanoTime() - start);
            return result;
        }

        @Override
        public Iterator<Instrument> iterator() {
            long start = System.nanoTime();
            Iterator<Instrument> result = target.iterator();
            System.out.printf("iterator() выполнен за %d нс%n", System.nanoTime() - start);
            return result;
        }
    }
}
