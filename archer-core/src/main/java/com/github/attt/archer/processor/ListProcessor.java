package com.github.attt.archer.processor;

import com.github.attt.archer.annotation.CacheList;
import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.metadata.ListCacheMetadata;
import com.github.attt.archer.operation.ListCacheOperation;
import com.github.attt.archer.processor.api.AbstractProcessor;
import com.github.attt.archer.processor.context.InvocationContext;
import com.github.attt.archer.roots.ListComponent;
import com.github.attt.archer.stats.event.CacheHitEvent;
import com.github.attt.archer.stats.event.CacheMissEvent;
import com.github.attt.archer.stats.event.CachePenetrationProtectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * List cache processor
 *
 * @param <V> cache value type
 * @author atpexgo.wu
 * @see ListCacheOperation
 * @see CacheList
 * @since 1.0
 */
public class ListProcessor<V> extends AbstractProcessor<ListCacheOperation<V>, Collection<V>> implements ListComponent {

    private final static Logger logger = LoggerFactory.getLogger(ListProcessor.class);

    @Override
    public Collection<V> get(InvocationContext context, ListCacheOperation<V> cacheOperation) {
        ListCacheMetadata metadata = cacheOperation.getMetadata();
        logger.debug("Get invocation context {}", context);
        if (metadata.getInvokeAnyway()) {
            // 'invokeAnyway' is set to true
            return loadAndPut(context, cacheOperation);
        }

        // fetch all object cache keys
        List<String> allCacheKeys;
        String key = generateCacheKey(context, metadata);
        Cache.Entry entry = cache.get(metadata.getArea(), key, cacheOperation.getCacheEventCollector());
        if (entry == null) {
            // cache is missing
            cacheOperation.getCacheEventCollector().collect(new CacheMissEvent());
            return loadAndPut(context, cacheOperation);
        } else if (entry.getValue() == null) {
            cacheOperation.getCacheEventCollector().collect(new CachePenetrationProtectedEvent());
            cacheOperation.getCacheEventCollector().collect(new CacheHitEvent());
            // cache value is null
            return null;
        } else {
            allCacheKeys = cacheOperation.getElementCacheKeySerializer().looseDeserialize(entry.getValue());
        }

        if (allCacheKeys == null) {
            // treat as cache is missing
            cacheOperation.getCacheEventCollector().collect(new CacheMissEvent());
            return loadAndPut(context, cacheOperation);
        }

        // fetch all object
        Map<String, Cache.Entry> allObjects = cache.getAll(metadata.getElementArea(), allCacheKeys, cacheOperation.getCacheEventCollector());

        Map<String, V> resultMap = new HashMap<>(allCacheKeys.size());
        for (Map.Entry<String, Cache.Entry> cacheEntry : allObjects.entrySet()) {
            String cacheObjectKey = cacheEntry.getKey();
            Cache.Entry cacheObject = cacheEntry.getValue();
            if (cacheObject == null) {
                // treat as cache is missing
                cacheOperation.getCacheEventCollector().collect(new CacheMissEvent());
                return loadAndPut(context, cacheOperation);
            }
            V deserialized;
            if (cacheObject.getValue() != null) {
                deserialized = cacheOperation.getValueSerializer().looseDeserialize(cacheObject.getValue());
                if (deserialized == null) {
                    // deserialized failed, treat as cache is missing
                    cacheOperation.getCacheEventCollector().collect(new CacheMissEvent());
                    return loadAndPut(context, cacheOperation);
                }
            } else {
                cacheOperation.getCacheEventCollector().collect(new CachePenetrationProtectedEvent());
                // cached object self is null, treat as null
                deserialized = null;
            }
            resultMap.put(cacheObjectKey, deserialized);
        }

        List<V> result = new ArrayList<>();
        // collect cache value in order
        for (String cacheKey : allCacheKeys) {
            result.add(resultMap.get(cacheKey));
        }

        cacheOperation.getCacheEventCollector().collect(new CacheHitEvent());
        return result;
    }

    @Override
    public Map<InvocationContext, Collection<V>> getAll(List<InvocationContext> contextList, ListCacheOperation<V> cacheOperation) {
        // may be more elegant? not to use looping
        Map<InvocationContext, Collection<V>> resultMap = new HashMap<>(contextList.size());
        for (InvocationContext context : contextList) {
            resultMap.put(context, get(context, cacheOperation));
        }
        return resultMap;
    }

    @Override
    public void put(InvocationContext context, Collection<V> values, ListCacheOperation<V> cacheOperation) {
        ListCacheMetadata metadata = cacheOperation.getMetadata();
        logger.debug("Put invocation context {}", context);

        String key = generateCacheKey(context, metadata);

        if (values == null) {
            cache.put(metadata.getArea(), key, cache.wrap(metadata.getArea(), key, null, metadata.getExpirationInMillis()), cacheOperation.getCacheEventCollector());
        } else {
            List<String> elementCacheKeys = new ArrayList<>();
            Map<String, Cache.Entry> elements = new HashMap<>();
            for (V value : values) {
                String elementKey = generateElementCacheKey(context, cacheOperation.getMetadata(), values, value);
                elements.put(elementKey, cache.wrap(metadata.getElementArea(), elementKey, cacheOperation.getValueSerializer().serialize(value), metadata.getExpirationInMillis()));
                elementCacheKeys.add(elementKey);
            }
            cache.putAllIfAbsent(metadata.getElementArea(), elements, cacheOperation.getCacheEventCollector());
            cache.put(metadata.getArea(), key, cache.wrap(metadata.getArea(), key, cacheOperation.getElementCacheKeySerializer().serialize(elementCacheKeys), metadata.getExpirationInMillis()), cacheOperation.getCacheEventCollector());
        }
    }

    @Override
    public void putAll(Map<InvocationContext, Collection<V>> contextValueMap, ListCacheOperation<V> cacheOperation) {
        // may be more elegant? not to use looping
        for (Map.Entry<? extends InvocationContext, ? extends Collection<V>> entry : contextValueMap.entrySet()) {
            put(entry.getKey(), entry.getValue(), cacheOperation);
        }
    }
}
