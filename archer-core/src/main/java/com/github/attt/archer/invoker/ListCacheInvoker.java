package com.github.attt.archer.invoker;

import com.github.attt.archer.annotation.CacheList;
import com.github.attt.archer.cache.Cache;
import com.github.attt.archer.annotation.metadata.ListCacheMetadata;
import com.github.attt.archer.annotation.config.ListCacheProperties;
import com.github.attt.archer.invoker.context.InvocationContext;
import com.github.attt.archer.roots.ListComponent;
import com.github.attt.archer.metrics.event.CacheHitEvent;
import com.github.attt.archer.metrics.event.CacheMissEvent;
import com.github.attt.archer.metrics.event.CachePenetrationProtectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * List cache invoker
 *
 * @param <V> cache value type
 * @author atpexgo.wu
 * @see ListCacheProperties
 * @see CacheList
 * @since 1.0
 */
public class ListCacheInvoker<V> extends AbstractInvoker<ListCacheProperties<V>, Collection<V>> implements ListComponent {

    private final static Logger logger = LoggerFactory.getLogger(ListCacheInvoker.class);

    @Override
    public Collection<V> get(InvocationContext context, ListCacheProperties<V> properties) {
        ListCacheMetadata metadata = properties.getMetadata();
        logger.debug("Get invocation context {}", context);
        if (metadata.getInvokeAnyway()) {
            // 'invokeAnyway' is set to true
            return loadAndPut(context, properties);
        }

        // fetch all object cache keys
        List<String> allCacheKeys;
        String key = generateCacheKey(context, metadata);
        Cache.Entry entry = cache.get(metadata.getRegion(), key, properties.getCacheEventCollector());
        if (entry == null) {
            // cache is missing
            properties.getCacheEventCollector().collect(new CacheMissEvent());
            return loadAndPut(context, properties);
        } else if (entry.getValue() == null) {
            properties.getCacheEventCollector().collect(new CachePenetrationProtectedEvent());
            properties.getCacheEventCollector().collect(new CacheHitEvent());
            // cache value is null
            return null;
        } else {
            allCacheKeys = properties.getElementCacheKeySerializer().looseDeserialize(entry.getValue());
        }

        if (allCacheKeys == null) {
            // treat as cache is missing
            properties.getCacheEventCollector().collect(new CacheMissEvent());
            return loadAndPut(context, properties);
        }

        // fetch all object
        Map<String, Cache.Entry> allObjects = cache.getAll(metadata.getElementArea(), allCacheKeys, properties.getCacheEventCollector());

        Map<String, V> resultMap = new HashMap<>(allCacheKeys.size());
        for (Map.Entry<String, Cache.Entry> cacheEntry : allObjects.entrySet()) {
            String cacheObjectKey = cacheEntry.getKey();
            Cache.Entry cacheObject = cacheEntry.getValue();
            if (cacheObject == null) {
                // treat as cache is missing
                properties.getCacheEventCollector().collect(new CacheMissEvent());
                return loadAndPut(context, properties);
            }
            V deserialized;
            if (cacheObject.getValue() != null) {
                deserialized = properties.getValueSerializer().looseDeserialize(cacheObject.getValue());
                if (deserialized == null) {
                    // deserialized failed, treat as cache is missing
                    properties.getCacheEventCollector().collect(new CacheMissEvent());
                    return loadAndPut(context, properties);
                }
            } else {
                properties.getCacheEventCollector().collect(new CachePenetrationProtectedEvent());
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

        properties.getCacheEventCollector().collect(new CacheHitEvent());
        return result;
    }

    @Override
    public Map<InvocationContext, Collection<V>> getAll(List<InvocationContext> contextList, ListCacheProperties<V> properties) {
        // may be more elegant? not to use looping
        Map<InvocationContext, Collection<V>> resultMap = new HashMap<>(contextList.size());
        for (InvocationContext context : contextList) {
            resultMap.put(context, get(context, properties));
        }
        return resultMap;
    }

    @Override
    public void put(InvocationContext context, Collection<V> values, ListCacheProperties<V> properties) {
        ListCacheMetadata metadata = properties.getMetadata();
        logger.debug("Put invocation context {}", context);

        String key = generateCacheKey(context, metadata);

        if (values == null) {
            cache.put(metadata.getRegion(), key, cache.wrap(metadata.getRegion(), key, null, metadata.getExpirationInMillis()), properties.getCacheEventCollector());
        } else {
            List<String> elementCacheKeys = new ArrayList<>();
            Map<String, Cache.Entry> elements = new HashMap<>();
            for (V value : values) {
                String elementKey = generateElementCacheKey(context, properties.getMetadata(), values, value);
                elements.put(elementKey, cache.wrap(metadata.getElementArea(), elementKey, properties.getValueSerializer().serialize(value), metadata.getElementExpirationInMillis()));
                elementCacheKeys.add(elementKey);
            }
            cache.putAllIfAbsent(metadata.getElementArea(), elements, properties.getCacheEventCollector());
            cache.put(metadata.getRegion(), key, cache.wrap(metadata.getRegion(), key, properties.getElementCacheKeySerializer().serialize(elementCacheKeys), metadata.getExpirationInMillis()), properties.getCacheEventCollector());

        }
    }

    @Override
    public void putAll(Map<InvocationContext, Collection<V>> contextValueMap, ListCacheProperties<V> properties) {
        // may be more elegant? not to use looping
        for (Map.Entry<? extends InvocationContext, ? extends Collection<V>> entry : contextValueMap.entrySet()) {
            put(entry.getKey(), entry.getValue(), properties);
        }
    }

}
