package com.atpex.archer.processor.api;

import com.atpex.archer.CacheManager;
import com.atpex.archer.annotation.Cache;
import com.atpex.archer.cache.internal.ShardingCache;
import com.atpex.archer.components.internal.InternalElementKeyGenerator;
import com.atpex.archer.components.internal.InternalKeyGenerator;
import com.atpex.archer.components.internal.InternalKeySerializer;
import com.atpex.archer.metadata.CacheMetadata;
import com.atpex.archer.stats.event.api.CacheEvent;
import com.atpex.archer.stats.event.collector.AnonymousCacheEventCollector;
import com.atpex.archer.stats.observer.CacheMetricsObserver;
import com.atpex.archer.operation.CacheOperation;
import com.atpex.archer.processor.context.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Abstract service cache processor
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public abstract class AbstractProcessor<C extends CacheOperation<?, V>, V> implements Processor<InvocationContext, C, V> {

    private final static Logger logger = LoggerFactory.getLogger(AbstractProcessor.class);

    protected final InternalKeyGenerator keyGenerator = new InternalKeyGenerator();

    protected final InternalElementKeyGenerator elementKeyGenerator = new InternalElementKeyGenerator();

    protected final InternalKeySerializer serializer = new InternalKeySerializer();

    protected final AnonymousCacheEventCollector anonymousCacheEventCollector = new AnonymousCacheEventCollector();

    protected ShardingCache cache;

    protected CacheMetricsObserver<CacheEvent> cacheObserver;

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
    public void delete(InvocationContext context, C cacheOperation) {
        cache.remove(generateCacheKey(context, cacheOperation.getMetadata()), cacheOperation.getCacheEventCollector());
    }

    /**
     * It's tricky way for {@link com.atpex.archer.invocation.CacheContext} to remove
     * cache by key in string literal
     *
     * @param key
     */
    public void deleteWithKey(String key) {
        cache.remove(key, anonymousCacheEventCollector);
    }

    protected String generateCacheKey(InvocationContext context, CacheMetadata metadata) {
        return keyGenerator.generateKey(metadata, context.getTarget(), context.getMethod(), context.getArgs(), null, null);
    }

    protected String generateCacheKey(InvocationContext context, CacheMetadata metadata, Object result) {
        return keyGenerator.generateKey(metadata, context.getTarget(), context.getMethod(), context.getArgs(), result, null);
    }

    protected String generateCacheKey(InvocationContext context, CacheMetadata metadata, Object result, Object resultElement) {
        return keyGenerator.generateKey(metadata, context.getTarget(), context.getMethod(), context.getArgs(), result, resultElement);
    }

    protected String generateElementCacheKey(InvocationContext context, CacheMetadata metadata, Object result, Object resultElement) {
        return elementKeyGenerator.generateKey(metadata, context.getTarget(), context.getMethod(), context.getArgs(), result, resultElement);
    }

    /**
     * Get result by invoking loader
     * <p>
     * If {@link Cache#breakdownProtect()}}
     * is true, only one of concurrent requests will
     * actually load result by invoking loader, other requests
     * will hold on until loading-request is done or timeout
     * {@link Cache#breakdownProtectTimeout()}
     *
     * @param context
     * @return
     * @see com.atpex.archer.annotation.CacheMulti
     * @see com.atpex.archer.annotation.CacheList
     * @see Cache
     */
    protected V loadAndPut(InvocationContext context, C cacheOperation) {
        CacheMetadata cacheMetadata = cacheOperation.getMetadata();
        logger.debug("Cache miss");
        if (cacheMetadata.getBreakdownProtect()) {
            logger.debug("Breakdown protect is on, loadAndPut synchronized");
            // synchronized loadAndPut
            return synchronizedLoadAndPut(context, cacheOperation);
        } else {
            logger.debug("Breakdown protect is off, loadAndPut");
            // loadAndPut
            return loadAndPut0(context, cacheOperation);
        }
    }

    private V loadAndPut0(InvocationContext context, C cacheOperation) {
        V load = load(context, cacheOperation);
        put(context, load, cacheOperation);
        return load;
    }

    private V load(InvocationContext context, C cacheOperation) {
        return cacheOperation.getLoader().load(context);
    }


    private V synchronizedLoadAndPut(InvocationContext context, C cacheOperation) {
        return doSynchronizedLoadAndPut(generateLockKey(context, cacheOperation.getMetadata()),
                cacheOperation.getMetadata().getBreakdownProtectTimeoutInMillis(),
                () -> load(context, cacheOperation),
                v -> put(context, v, cacheOperation),
                () -> loadAndPut0(context, cacheOperation));
    }

    private <V0> V0 doSynchronizedLoadAndPut(String lockKey, long breakdownTimeout, Supplier<V0> load, Consumer<V0> put, Supplier<V0> loadAndPut) {
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
                }
                if (bpl.success) {
                    return (V0) bpl.value;
                }
            }
        }
    }

    protected Map<InvocationContext, V> loadAndPutAll(List<InvocationContext> contextList, C cacheOperation) {
        CacheMetadata metadata = cacheOperation.getMetadata();
        logger.debug("Cache miss");
        if (metadata.getBreakdownProtect()) {
            logger.debug("Breakdown protect is on, loadAndPutAll synchronized");
            // synchronized loadAndPut
            return synchronizedLoadAndPutAll(contextList, cacheOperation);
        } else {
            logger.debug("Breakdown protect is off, loadAndPutAll");
            // loadAndPut
            return loadAndPutAll0(contextList, cacheOperation);
        }
    }

    private Map<InvocationContext, V> loadAll(List<InvocationContext> contexts, C cacheOperation) {
        // actually single loader and multiple loader won't be set at the same time
        // but in case, use multiple loader first
        if (cacheOperation.getMultipleLoader() != null) {
            return cacheOperation.getMultipleLoader().load(contexts);
        }
        Map<InvocationContext, V> result = new HashMap<>(contexts.size());
        for (InvocationContext context : contexts) {
            result.put(context, cacheOperation.getLoader().load(context));
        }
        return result;
    }

    private Map<InvocationContext, V> loadAndPutAll0(List<InvocationContext> contexts, C cacheOperation) {
        Map<InvocationContext, V> load = loadAll(contexts, cacheOperation);
        putAll(load, cacheOperation);
        return load;
    }

    private Map<InvocationContext, V> synchronizedLoadAndPutAll(List<InvocationContext> contexts, C cacheOperation) {
        return doSynchronizedLoadAndPut(generateLockKey(contexts, cacheOperation.getMetadata()),
                cacheOperation.getMetadata().getBreakdownProtectTimeoutInMillis(),
                () -> loadAll(contexts, cacheOperation),
                v -> putAll(v, cacheOperation),
                () -> loadAndPutAll0(contexts, cacheOperation));
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
        //
        cacheObserver = cacheManager.getCacheObserver();
        cache = cacheManager.getShardingCache();
    }
}