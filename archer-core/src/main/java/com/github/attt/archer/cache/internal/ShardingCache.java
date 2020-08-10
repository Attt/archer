package com.github.attt.archer.cache.internal;


import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.cache.api.ShardingConfigure;
import com.github.attt.archer.constants.Constants;
import com.github.attt.archer.stats.api.CacheEventCollector;
import com.github.attt.archer.stats.event.CacheAccessEvent;
import com.github.attt.archer.stats.event.CachePositivelyEvictEvent;
import com.github.attt.archer.stats.event.CacheTimeElapsingEvent;
import com.github.attt.archer.util.CommonUtils;

import java.util.*;

/**
 * Sharding cache operation source
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class ShardingCache implements Cache {

    private ShardingConfigure shardingConfigure;

    public static final String SHARDING_CACHE_BEAN_NAME = "archer.cache.internalCache";

    public ShardingCache(ShardingConfigure shardingConfigure) {
        this.shardingConfigure = shardingConfigure;
    }

    public void setShardingConfigure(ShardingConfigure shardingConfigure) {
        this.shardingConfigure = shardingConfigure;
    }

    @Override
    public boolean containsKey(String area, String key, CacheEventCollector collector) {
        key = prefixKey(area, key);
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        CacheAccessEvent cacheAccessEvent = new CacheAccessEvent();
        boolean exist = shardingConfigure.sharding(key).containsKey(area, key, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        collector.collect(cacheAccessEvent);
        return exist;
    }

    @Override
    public Entry get(String area, String key, CacheEventCollector collector) {
        key = prefixKey(area, key);
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        Entry entry = shardingConfigure.sharding(key).get(area, key, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        return entry;
    }

    @Override
    public Map<String, Entry> getAll(String area, Collection<String> keys, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        Map<Cache, List<String>> shardingKeys = new LinkedHashMap<>();
        for (String key : keys) {
            key = prefixKey(area, key);
            Cache cache = shardingConfigure.sharding(key);
            shardingKeys.computeIfAbsent(cache, cacheKey -> new ArrayList<>()).add(key);
        }

        // make sure list order is right
        Map<String, Entry> keysToResult = new HashMap<>();
        for (Map.Entry<Cache, List<String>> operationSourceKeysEntry : shardingKeys.entrySet()) {
            Cache cache = operationSourceKeysEntry.getKey();
            List<String> keysInOneShard = operationSourceKeysEntry.getValue();
            if (CommonUtils.isEmpty(keysInOneShard)) {
                continue;
            }
            Map<String, Entry> entriesInOneShard = cache.getAll(area, keysInOneShard, collector);
            keysToResult.putAll(entriesInOneShard);
        }
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        return keysToResult;
    }

    @Override
    public void put(String area, String key, Entry value, CacheEventCollector collector) {
        key = prefixKey(area, key);
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        shardingConfigure.sharding(key).put(area, key, value, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
    }

    @Override
    public void putAll(String area, Map<String, Entry> map, CacheEventCollector collector) {
        doPutAll(area, map, collector, false);
    }

    @Override
    public void putIfAbsent(String area, String key, Entry value, CacheEventCollector collector) {
        key = prefixKey(area, key);
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        shardingConfigure.sharding(key).putIfAbsent(area, key, value, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
    }

    @Override
    public void putAllIfAbsent(String area, Map<String, Entry> map, CacheEventCollector collector) {
        doPutAll(area, map, collector, true);
    }

    private void doPutAll(String area, Map<String, Entry> map, CacheEventCollector collector, boolean considerAbsent) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        Map<Cache, Map<String, Entry>> shardingKeys = new HashMap<>();
        for (Map.Entry<String, Entry> kv : map.entrySet()) {
            String key = prefixKey(area, kv.getKey());
            Entry value = kv.getValue();
            Cache cache = shardingConfigure.sharding(key);
            if (shardingKeys.containsKey(cache)) {
                shardingKeys.get(cache).put(key, value);
            } else {
                Map<String, Entry> keyValuesInOneShard = new HashMap<>();
                keyValuesInOneShard.put(key, value);
                shardingKeys.put(cache, keyValuesInOneShard);
            }
        }

        for (Map.Entry<Cache, Map<String, Entry>> cacheEntry : shardingKeys.entrySet()) {
            Cache cache = cacheEntry.getKey();
            Map<String, Entry> keyValuesInOneShard = cacheEntry.getValue();
            if (considerAbsent) {
                cache.putAllIfAbsent(area, keyValuesInOneShard, collector);
            } else {
                cache.putAll(area, keyValuesInOneShard, collector);
            }
        }
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
    }

    @Override
    public boolean remove(String area, String key, CacheEventCollector collector) {
        key = prefixKey(area, key);
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        CachePositivelyEvictEvent cachePositivelyEvictEvent = new CachePositivelyEvictEvent();
        boolean remove = shardingConfigure.sharding(key).remove(area, key, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        collector.collect(cachePositivelyEvictEvent);
        return remove;
    }

    @Override
    public boolean removeAll(String area, Collection<String> keys, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        CachePositivelyEvictEvent cachePositivelyEvictEvent = new CachePositivelyEvictEvent();
        boolean remove = true;
        Map<Cache, List<String>> shardingKeys = new HashMap<>();
        for (String key : keys) {
            key = prefixKey(area, key);
            Cache cache = shardingConfigure.sharding(key);
            String finalKey = key;
            shardingKeys.compute(cache, (c, ks) -> {
                if (ks == null) {
                    ks = new ArrayList<>();
                }
                ks.add(finalKey);
                return ks;
            });
        }

        for (Map.Entry<Cache, List<String>> cacheEntry : shardingKeys.entrySet()) {
            Cache cache = cacheEntry.getKey();
            List<String> keysInOneShard = cacheEntry.getValue();
            remove = remove && cache.removeAll(area, keysInOneShard, collector);
        }
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        collector.collect(cachePositivelyEvictEvent);
        return remove;
    }

    @Override
    public boolean removeAll(String area, CacheEventCollector collector) {
        boolean re = true;
        while (shardingConfigure.shards().hasNext()) {
            re = re && shardingConfigure.shards().next().removeAll(area, collector);
        }
        return re;
    }

    @Override
    public Entry wrap(String area, String key, byte[] value, long ttl) {
        key = prefixKey(area, key);
        return shardingConfigure.sharding(key).wrap(area, key, value, ttl);
    }

    public String prefixKey(String area, String key) {
        return CommonUtils.isNotEmpty(area) ? area + Constants.DEFAULT_DELIMITER + key : key;
    }
}
