package com.github.attt.archer.cache.caffeine;

import com.github.attt.archer.cache.CacheConfig;
import com.github.attt.archer.cache.HashMapCache;
import com.github.attt.archer.metrics.api.CacheEventCollector;
import com.github.attt.archer.metrics.event.CacheAccessEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public final class CaffeineCache extends HashMapCache {

    private Cache<String, Entry> cache;

    private Cache<String, Set<String>> areaKeysCache;

    @Override
    public void init(CacheConfig shard) {
        if (!(shard instanceof CaffeineConfig)) {
            throw new RuntimeException("Cache config info supplied is not a instance of com.github.attt.archer.cache.caffeine.CaffeineConfig");
        }
        cache = Caffeine.newBuilder()
                .initialCapacity(((CaffeineConfig) shard).getInitialCapacity())
                .maximumSize(((CaffeineConfig) shard).getMaximumSize())
                .expireAfterWrite(((CaffeineConfig) shard).getExpireAfterWriteInSecond(), TimeUnit.SECONDS)
                .build();

        areaKeysCache = Caffeine.newBuilder().build();
    }

    @Override
    public boolean containsKey(String region, String key, CacheEventCollector collector) {
        boolean exists = cache.getIfPresent(key) != null;
        collector.collect(new CacheAccessEvent());
        return exists;
    }

    @Override
    public Map<String, Entry> getAll(String region, Collection<String> keys, CacheEventCollector collector) {
        Map<String, Entry> allPresent = cache.getAllPresent(keys);
        collector.collect(new CacheAccessEvent());
        for (String key : keys) {
            if (!allPresent.containsKey(key)) {
                allPresent.put(key, null);
            }
        }
        return allPresent;
    }

    @Override
    public void put(String region, String key, Entry value, CacheEventCollector collector) {
        areaKeysCache.asMap().compute(region, (area1, keys) -> {
            if (keys == null) {
                keys = new HashSet<>();
            }
            keys.add(key);
            return keys;
        });
        cache.put(key, value);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putAll(String region, Map<String, Entry> map, CacheEventCollector collector) {
        areaKeysCache.asMap().compute(region, (area1, keys) -> {
            if (keys == null) {
                keys = new HashSet<>();
            }
            keys.addAll(map.keySet());
            return keys;
        });
        cache.putAll(map);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putIfAbsent(String region, String key, Entry value, CacheEventCollector collector) {
        areaKeysCache.asMap().compute(region, (area1, keys) -> {
            if (keys == null) {
                keys = new HashSet<>();
            }
            keys.add(key);
            return keys;
        });
        cache.get(key, s -> {
            Entry present = cache.getIfPresent(s);
            collector.collect(new CacheAccessEvent());
            if (present == null) {
                cache.put(key, value);
                collector.collect(new CacheAccessEvent());
                return value;
            }
            return present;
        });
    }

    @Override
    public boolean remove(String region, String key, CacheEventCollector collector) {
        cache.invalidate(key);
        areaKeysCache.asMap().compute(region, (area1, keys) -> {
            if (keys != null) {
                keys.remove(key);
            }
            return keys;
        });
        collector.collect(new CacheAccessEvent());
        return false;
    }

    @Override
    public boolean removeAll(String region, Collection<String> keys, CacheEventCollector collector) {
        cache.invalidateAll(keys);
        areaKeysCache.asMap().compute(region, (area1, keys1) -> {
            if (keys1 != null) {
                for (String key : keys) {
                    keys1.remove(key);
                }
            }
            return keys1;
        });
        collector.collect(new CacheAccessEvent());
        return false;
    }

    @Override
    public boolean removeAll(String region, CacheEventCollector collector) {
        Set<String> keys = areaKeysCache.asMap().keySet();
        for (String key : keys) {
            cache.invalidate(key);
            areaKeysCache.invalidate(key);
        }
        return false;
    }


    @Override
    public void putAllIfAbsent(String region, Map<String, Entry> map, CacheEventCollector collector) {
        map.forEach((k, e) -> putIfAbsent(region, k, e, collector));
    }

    @Override
    public Entry get(String region, String key, CacheEventCollector collector) {
        Entry entry = cache.getIfPresent(key);
        collector.collect(new CacheAccessEvent());
        return entry;
    }
}
