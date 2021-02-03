package com.github.attt.archer.invoker;

import com.github.attt.archer.CacheManager;
import com.github.attt.archer.annotation.Cache;
import com.github.attt.archer.annotation.CacheList;
import com.github.attt.archer.annotation.Protection;
import com.github.attt.archer.cache.ShardingCache;
import com.github.attt.archer.cache.internal.InternalElementKeyGenerator;
import com.github.attt.archer.cache.internal.InternalKeyGenerator;
import com.github.attt.archer.annotation.metadata.CacheMetadata;
import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;
import com.github.attt.archer.annotation.config.EvictionProperties;
import com.github.attt.archer.annotation.config.AbstractCacheProperties;
import com.github.attt.archer.invoker.context.InvocationContext;
import com.github.attt.archer.metrics.api.CacheEvent;
import com.github.attt.archer.metrics.api.CacheEventCollector;
import com.github.attt.archer.metrics.api.listener.CacheMetricsListener;
import com.github.attt.archer.metrics.collector.NamedCacheEventCollector;
import com.github.attt.archer.metrics.event.CacheBreakdownProtectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.github.attt.archer.constants.Constants.DEFAULT_DELIMITER;


/**
 * Abstract service cache processor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public abstract class AbstractInvoker<C extends AbstractCacheProperties<?, V>, V> implements Invoker<InvocationContext, C, EvictionProperties, V>, ManualInvoker {

    private final static Logger logger = LoggerFactory.getLogger(AbstractInvoker.class);

    private final InternalKeyGenerator keyGenerator = new InternalKeyGenerator();

    private final InternalElementKeyGenerator elementKeyGenerator = new InternalElementKeyGenerator();

    private final NamedCacheEventCollector manualCacheEventCollector = new NamedCacheEventCollector("manual");

    protected ShardingCache cache;

    private volatile ConcurrentHashMap<Object, BreakdownProtectionLock> loaderMap;

    static class BreakdownProtectionLock {
        CountDownLatch signal;
        Thread loaderThread;
        volatile boolean success;
        volatile Object value;
    }

    private ConcurrentHashMap<Object, BreakdownProtectionLock> initOrGetLoaderMap() {
        if (loaderMap == null) {
            synchronized (this) {
                if (loaderMap == null) {
                    loaderMap = new ConcurrentHashMap<>();
                }
            }
        }
        return loaderMap;
    }

    @Override
    public void delete(InvocationContext context, EvictionProperties evictionProperties) {
        cache.remove(evictionProperties.getMetadata().getRegion(), generateCacheKey(context, evictionProperties.getMetadata()), evictionProperties.getCacheEventCollector());
    }

    @Override
    public void deleteAll(List<InvocationContext> contextList, EvictionProperties evictionProperties) {
        List<String> keys = new ArrayList<>();
        for (InvocationContext context : contextList) {
            keys.add(generateCacheKey(context, evictionProperties.getMetadata()));
        }
        cache.removeAll(evictionProperties.getMetadata().getRegion(), keys, evictionProperties.getCacheEventCollector());
    }

    @Override
    public void deleteAll(EvictionProperties evictionProperties) {
        cache.removeAll(evictionProperties.getMetadata().getRegion(), evictionProperties.getCacheEventCollector());
    }

    @Override
    public void delete(String area, String key) {
        cache.remove(area, area + DEFAULT_DELIMITER + key, manualCacheEventCollector);
    }

    @Override
    public void deleteAll(String area) {
        cache.removeAll(area, manualCacheEventCollector);
    }

    @Override
    public boolean exist(String area, String key) {
        return cache.containsKey(area, area + DEFAULT_DELIMITER + key, manualCacheEventCollector);
    }

    protected String generateCacheKey(InvocationContext context, AbstractCacheMetadata metadata) {
        return keyGenerator.generateKey(metadata, context.getTarget(), context.getMethod(), context.getArgs(), null, null);
    }

    protected String generateCacheKey(InvocationContext context, AbstractCacheMetadata metadata, Object result) {
        return keyGenerator.generateKey(metadata, context.getTarget(), context.getMethod(), context.getArgs(), result, null);
    }

    protected String generateCacheKey(InvocationContext context, AbstractCacheMetadata metadata, Object result, Object resultElement) {
        return keyGenerator.generateKey(metadata, context.getTarget(), context.getMethod(), context.getArgs(), result, resultElement);
    }

    protected String generateElementCacheKey(InvocationContext context, AbstractCacheMetadata metadata, Object result, Object resultElement) {
        return elementKeyGenerator.generateKey(metadata, context.getTarget(), context.getMethod(), context.getArgs(), result, resultElement);
    }

    /**
     * Get result by invoking loader
     * <p>
     * If {@link Protection#breakdownProtect()}
     * is true, only one of concurrent requests will
     * actually load result by invoking loader, other requests
     * will hold on until loading-request is done or timeout
     * {@link Protection#breakdownProtectTimeout()}
     *
     * @param context
     * @return
     * @see CacheList
     * @see Cache
     */
    protected V loadAndPut(InvocationContext context, C cacheProperties) {
        CacheMetadata cacheMetadata = cacheProperties.getMetadata();
        logger.debug("Cache miss");
        if (cacheMetadata.getBreakdownProtect()) {
            logger.debug("Breakdown protect is on, loadAndPut synchronized");
            // synchronized loadAndPut
            return synchronizedLoadAndPut(context, cacheProperties);
        } else {
            logger.debug("Breakdown protect is off, loadAndPut");
            // loadAndPut
            return loadAndPut0(context, cacheProperties);
        }
    }

    private V loadAndPut0(InvocationContext context, C cacheProperties) {
        V load = load(context, cacheProperties);
        put(context, load, cacheProperties);
        return load;
    }

    private V load(InvocationContext context, C cacheProperties) {
        return cacheProperties.getLoader().load(context);
    }


    private V synchronizedLoadAndPut(InvocationContext context, C cacheProperties) {
        return doSynchronizedLoadAndPut(generateLockKey(context, cacheProperties.getMetadata()),
                cacheProperties.getMetadata().getBreakdownProtectTimeoutInMillis(),
                () -> load(context, cacheProperties),
                v -> put(context, v, cacheProperties),
                () -> loadAndPut0(context, cacheProperties),
                cacheProperties.getCacheEventCollector());
    }

    private <V0> V0 doSynchronizedLoadAndPut(String lockKey, long breakdownTimeout, Supplier<V0> load, Consumer<V0> put, Supplier<V0> loadAndPut, CacheEventCollector cacheEventCollector) {
        ConcurrentHashMap<Object, BreakdownProtectionLock> loaderMap = initOrGetLoaderMap();
        while (true) {
            boolean[] created = new boolean[1];
            BreakdownProtectionLock bpl = loaderMap.computeIfAbsent(lockKey, (unusedKey) -> {
                created[0] = true;
                BreakdownProtectionLock loadingLock = new BreakdownProtectionLock();
                loadingLock.signal = new CountDownLatch(1);
                loadingLock.loaderThread = Thread.currentThread();
                return loadingLock;
            });
            if (created[0] || bpl.loaderThread == Thread.currentThread()) {
                try {
                    V0 loadedValue = load.get();
                    bpl.success = true;
                    bpl.value = loadedValue;
                    put.accept(loadedValue);
                    return loadedValue;
                } finally {
                    if (created[0]) {
                        bpl.signal.countDown();
                        loaderMap.remove(lockKey);
                    }
                }
            } else {
                CacheBreakdownProtectedEvent cacheBreakdownProtectedEvent = new CacheBreakdownProtectedEvent();
                try {
                    if (breakdownTimeout <= 0) {
                        bpl.signal.await();
                    } else {
                        boolean ok = bpl.signal.await(breakdownTimeout, TimeUnit.MILLISECONDS);
                        if (!ok) {
                            logger.debug("loader wait timeout:" + breakdownTimeout);
                            return loadAndPut.get();
                        }
                    }
                } catch (InterruptedException e) {
                    logger.debug("loader wait interrupted");
                    return loadAndPut.get();
                } finally {
                    cacheEventCollector.collect(cacheBreakdownProtectedEvent);
                }
                if (bpl.success) {
                    return (V0) bpl.value;
                }
            }
        }
    }

    protected Map<InvocationContext, V> loadAndPutAll(List<InvocationContext> contextList, C cacheProperties) {
        CacheMetadata metadata = cacheProperties.getMetadata();
        logger.debug("Cache miss");
        if (metadata.getBreakdownProtect()) {
            logger.debug("Breakdown protect is on, loadAndPutAll synchronized");
            // synchronized loadAndPut
            return synchronizedLoadAndPutAll(contextList, cacheProperties);
        } else {
            logger.debug("Breakdown protect is off, loadAndPutAll");
            // loadAndPut
            return loadAndPutAll0(contextList, cacheProperties);
        }
    }

    private Map<InvocationContext, V> loadAll(List<InvocationContext> contexts, C cacheProperties) {
        // actually single loader and multiple loader won't be set at the same time
        // but in case, use multiple loader first
        if (cacheProperties.getMultipleLoader() != null) {
            return cacheProperties.getMultipleLoader().load(contexts);
        }
        Map<InvocationContext, V> result = new HashMap<>(contexts.size());
        for (InvocationContext context : contexts) {
            result.put(context, cacheProperties.getLoader().load(context));
        }
        return result;
    }

    private Map<InvocationContext, V> loadAndPutAll0(List<InvocationContext> contexts, C cacheProperties) {
        Map<InvocationContext, V> load = loadAll(contexts, cacheProperties);
        putAll(load, cacheProperties);
        return load;
    }

    private Map<InvocationContext, V> synchronizedLoadAndPutAll(List<InvocationContext> contexts, C cacheProperties) {
        return doSynchronizedLoadAndPut(generateLockKey(contexts, cacheProperties.getMetadata()),
                cacheProperties.getMetadata().getBreakdownProtectTimeoutInMillis(),
                () -> loadAll(contexts, cacheProperties),
                v -> putAll(v, cacheProperties),
                () -> loadAndPutAll0(contexts, cacheProperties),
                cacheProperties.getCacheEventCollector());
    }


    private String generateLockKey(List<InvocationContext> contexts, CacheMetadata metadata) {
        StringBuilder stringBuilder = new StringBuilder();
        for (InvocationContext context : contexts) {
            stringBuilder.append(generateCacheKey(context, metadata)).append("_");
        }
        return stringBuilder.toString();
    }

    private String generateLockKey(InvocationContext context, CacheMetadata metadata) {
        return generateCacheKey(context, metadata);
    }

    public void afterInitialized(CacheManager cacheManager) {
        for (CacheMetricsListener<CacheEvent> statsListener : cacheManager.getStatsListenerMap().values()) {
            manualCacheEventCollector.register(statsListener);
        }
        cache = cacheManager.getShardingCache();
    }
}
