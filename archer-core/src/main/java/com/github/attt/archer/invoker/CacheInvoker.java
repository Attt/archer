package com.github.attt.archer.invoker;

import com.github.attt.archer.cache.Cache;
import com.github.attt.archer.exception.FallbackException;
import com.github.attt.archer.annotation.metadata.ObjectCacheMetadata;
import com.github.attt.archer.annotation.config.ObjectCacheProperties;
import com.github.attt.archer.invoker.context.InvocationContext;
import com.github.attt.archer.roots.ObjectComponent;
import com.github.attt.archer.metrics.event.CacheHitEvent;
import com.github.attt.archer.metrics.event.CacheMissEvent;
import com.github.attt.archer.metrics.event.CachePenetrationProtectedEvent;
import com.github.attt.archer.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Object cache invoker
 *
 * @param <V> cache value type
 * @author atpexgo.wu
 * @see ObjectCacheProperties
 * @see com.github.attt.archer.annotation.Cache
 * @since 1.0
 */
public class CacheInvoker<V> extends AbstractInvoker<ObjectCacheProperties<V>, V> implements ObjectComponent {

    private final static Logger logger = LoggerFactory.getLogger(CacheInvoker.class);

    @Override
    public V get(InvocationContext context, ObjectCacheProperties<V> properties) {
        ObjectCacheMetadata metadata = properties.getMetadata();
        logger.debug("Get invocation context {}", context);
        if (metadata.getInvokeAnyway()) {
            return loadAndPut(context, properties);
        }
        String key = generateCacheKey(context, metadata);
        Cache.Entry entry = cache.get(metadata.getRegion(), key, properties.getCacheEventCollector());
        if (entry == null) {
            // cache is missing
            properties.getCacheEventCollector().collect(new CacheMissEvent());
            return loadAndPut(context, properties);
        } else if (entry.getValue() == null) {
            properties.getCacheEventCollector().collect(new CachePenetrationProtectedEvent());
            logger.debug("Cache hit, value is NULL");
            properties.getCacheEventCollector().collect(new CacheHitEvent());
            // cached but value is really set to NULL
            return null;
        }

        // try to deserialize
        V value = properties.getValueSerializer().looseDeserialize(entry.getValue());
        if (value == null) {
            return loadAndPut(context, properties);
        }

        logger.debug("Cache hit");
        properties.getCacheEventCollector().collect(new CacheHitEvent());
        return value;
    }

    @Override
    public Map<InvocationContext, V> getAll(List<InvocationContext> invocationContexts, ObjectCacheProperties<V> properties) {
        ObjectCacheMetadata metadata = properties.getMetadata();
        if (CommonUtils.isEmpty(invocationContexts)) {
            return new LinkedHashMap<>();
        }

        logger.debug("GetAll invocation context {}", Arrays.toString(invocationContexts.toArray()));
        Map<InvocationContext, Cache.Entry> resultEntryMap = new LinkedHashMap<>(invocationContexts.size());
        List<String> keys = new ArrayList<>();
        for (InvocationContext context : invocationContexts) {
            keys.add(generateCacheKey(context, metadata));
        }

        List<InvocationContext> contextList = new ArrayList<>(invocationContexts);
        Map<String, Cache.Entry> entryMap = metadata.getInvokeAnyway() ? null : cache.getAll(metadata.getRegion(), keys, properties.getCacheEventCollector());

        if (CommonUtils.isEmpty(entryMap)) {
            logger.debug("No entity in cache found..., load all");
            properties.getCacheEventCollector().collect(new CacheMissEvent());
            return loadAndPutAll(invocationContexts, properties);
        }

        List<InvocationContext> missedContextList = new ArrayList<>();

        int missCount = 0;

        // cache key order
        Map<InvocationContext, V> deserializedValueCache = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Cache.Entry entry = entryMap.get(key);
            InvocationContext context = contextList.get(i);
            V deserializedValue = null;
            boolean isCacheMissing = entry == null;
            boolean isCacheNull = !isCacheMissing && entry.getValue() == null;
            boolean isCacheDeserializingFail = !isCacheMissing && !isCacheNull && (deserializedValue = properties.getValueSerializer().looseDeserialize(entry.getValue())) == null;
            deserializedValueCache.put(context, deserializedValue);
            if (isCacheMissing || isCacheDeserializingFail) {
                missCount++;
                missedContextList.add(context);
                properties.getCacheEventCollector().collect(new CacheMissEvent());
                // take place first, keep the order right
                resultEntryMap.put(context, null);
            } else {
                properties.getCacheEventCollector().collect(new CacheHitEvent());
                resultEntryMap.put(context, entry);
            }
        }

        Map<InvocationContext, V> loadedResult = new HashMap<>(missedContextList.size());

        if (!missedContextList.isEmpty()) {
            loadedResult = loadAndPutAll(invocationContexts, properties);
        }

        // if noise data exists, load method every time
        if (missCount > 0 && loadedResult.size() < missCount && loadedResult.containsKey(null)) {
            // load from method;
            properties.getCacheEventCollector().collect(new CacheMissEvent());
            throw new FallbackException();
        }

        LinkedHashMap<InvocationContext, V> combinedOrdered = new LinkedHashMap<>(invocationContexts.size());
        for (Map.Entry<InvocationContext, Cache.Entry> resultEntry : resultEntryMap.entrySet()) {
            if (!loadedResult.containsKey(resultEntry.getKey()) && missedContextList.contains(resultEntry.getKey())) {
                // absent in loaded result
                continue;
            }
            if (resultEntry.getValue() == null) {
                combinedOrdered.put(resultEntry.getKey(), loadedResult.get(resultEntry.getKey()));
            } else {
                combinedOrdered.put(resultEntry.getKey(), deserializedValueCache.get(resultEntry.getKey()));
            }
        }

        logger.debug("{} missed/{} hit", missCount, keys.size() - missCount);
        return combinedOrdered;
    }

    @Override
    public void put(InvocationContext context, V value, ObjectCacheProperties<V> properties) {
        ObjectCacheMetadata metadata = properties.getMetadata();
        logger.debug("Put invocation context {}", context);
        String key = generateCacheKey(context, metadata);
        Cache.Entry entry = cache.wrap(metadata.getRegion(), key, properties.getValueSerializer().serialize(value), metadata.getExpirationInMillis());
        cache.put(metadata.getRegion(), key, entry, properties.getCacheEventCollector());
    }

    @Override
    public void putAll(Map<InvocationContext, V> contextValueMap, ObjectCacheProperties<V> properties) {
        ObjectCacheMetadata metadata = properties.getMetadata();
        Map<String, Cache.Entry> kvMap = new HashMap<>(contextValueMap.size());
        for (Map.Entry<? extends InvocationContext, ? extends V> contextEntry : contextValueMap.entrySet()) {
            if (contextEntry.getKey() == null) {
                // noise data
                continue;
            }
            String key = generateCacheKey(contextEntry.getKey(), metadata);
            byte[] serializedValue = properties.getValueSerializer().serialize(contextEntry.getValue());
            Cache.Entry entry = cache.wrap(metadata.getRegion(), key, serializedValue, metadata.getExpirationInMillis());
            kvMap.put(key, entry);
        }
        if (CommonUtils.isNotEmpty(kvMap)) {
            cache.putAll(metadata.getRegion(), kvMap, properties.getCacheEventCollector());
        }
    }
}
