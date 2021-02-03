package com.github.attt.archer.cache;


import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.metrics.api.CacheEventCollector;
import com.github.attt.archer.metrics.event.CacheAccessEvent;
import com.github.attt.archer.metrics.event.CacheActivelyEvictEvent;
import com.github.attt.archer.metrics.event.CacheTimeElapsingEvent;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.ShardingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Sharding cache
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class ShardingCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(ShardingCache.class);

    public static final String SHARDING_CACHE_BEAN_NAME = "archer.cache.internalCache";

    /**
     * 每个物理节点的虚拟节点数量
     */
    private static final int VNODE_NUM = 50;

    protected volatile Boolean initialized = false;

    private TreeMap<Long, Cache> shardingNodes;

    private final HashMapCache defaultCache = new HashMapCache();

    public ShardingCache(CacheFactory cacheFactory, List<CacheConfig> cacheConfigList) {
        if (!CommonUtils.isEmpty(cacheConfigList)) {
            init(cacheFactory, cacheConfigList);
        }
    }

    private void init(CacheFactory cacheFactory, List<CacheConfig> cacheConfigList) {
        shardingNodes = new TreeMap<>();
        if (cacheFactory != null) {
            log.debug("Initializing cache of factory : {}", cacheFactory.getClass().getName());

            for (int i = 0; i < cacheConfigList.size(); i++) {
                CacheConfig shard = cacheConfigList.get(i);
                try {
                    Cache cache = cacheFactory.initial(shard);
                    for (int j = 0; j < VNODE_NUM; j++) {
                        shardingNodes.put(ShardingUtil.hash("SHARD-" + i + "NODE-" + j), cache);
                    }
                } catch (Throwable t) {
                    throw new CacheBeanParsingException("Create cache failed", t);
                }
            }
        }
        initialized = true;
    }

    private Cache sharding(String seed) {
        if (!initialized) {
            return defaultCache;
        }
        SortedMap<Long, Cache> tail = shardingNodes.tailMap(ShardingUtil.hash(seed));
        if (tail.size() == 0) {
            return shardingNodes.get(shardingNodes.firstKey());
        }
        return tail.get(tail.firstKey());
    }

    @Override
    public boolean containsKey(String region, String key, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        CacheAccessEvent cacheAccessEvent = new CacheAccessEvent();
        boolean exist = sharding(key).containsKey(region, key, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        collector.collect(cacheAccessEvent);
        return exist;
    }

    @Override
    public Entry get(String region, String key, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        Entry entry = sharding(key).get(region, key, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        return entry;
    }

    @Override
    public Map<String, Entry> getAll(String region, Collection<String> keys, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        Map<Cache, List<String>> shardingKeys = new LinkedHashMap<>();
        for (String key : keys) {
            Cache cache = sharding(key);
            shardingKeys.computeIfAbsent(cache, cacheKey -> new ArrayList<>()).add(key);
        }

        // make sure list order is right
        Map<String, Entry> keysToResult = new HashMap<>();
        for (Map.Entry<Cache, List<String>> cacheKeysEntry : shardingKeys.entrySet()) {
            Cache cache = cacheKeysEntry.getKey();
            List<String> keysInOneShard = cacheKeysEntry.getValue();
            if (CommonUtils.isEmpty(keysInOneShard)) {
                continue;
            }
            Map<String, Entry> entriesInOneShard = cache.getAll(region, keysInOneShard, collector);
            keysToResult.putAll(entriesInOneShard);
        }
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        return keysToResult;
    }

    @Override
    public void put(String region, String key, Entry value, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        sharding(key).put(region, key, value, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
    }

    @Override
    public void putAll(String region, Map<String, Entry> map, CacheEventCollector collector) {
        doPutAll(region, map, collector, false);
    }

    @Override
    public void putIfAbsent(String region, String key, Entry value, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        sharding(key).putIfAbsent(region, key, value, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
    }

    @Override
    public void putAllIfAbsent(String region, Map<String, Entry> map, CacheEventCollector collector) {
        doPutAll(region, map, collector, true);
    }

    private void doPutAll(String region, Map<String, Entry> map, CacheEventCollector collector, boolean considerAbsent) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        Map<Cache, Map<String, Entry>> shardingKeys = new HashMap<>();
        for (Map.Entry<String, Entry> kv : map.entrySet()) {
            String key = kv.getKey();
            Entry value = kv.getValue();
            Cache cache = sharding(key);
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
                cache.putAllIfAbsent(region, keyValuesInOneShard, collector);
            } else {
                cache.putAll(region, keyValuesInOneShard, collector);
            }
        }
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
    }

    @Override
    public boolean remove(String region, String key, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        CacheActivelyEvictEvent cacheActivelyEvictEvent = new CacheActivelyEvictEvent();
        boolean remove = sharding(key).remove(region, key, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        collector.collect(cacheActivelyEvictEvent);
        return remove;
    }

    @Override
    public boolean removeAll(String region, Collection<String> keys, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        CacheActivelyEvictEvent cacheActivelyEvictEvent = new CacheActivelyEvictEvent();
        boolean remove = true;
        Map<Cache, List<String>> shardingKeys = new HashMap<>();
        for (String key : keys) {
            Cache cache = sharding(key);
            shardingKeys.compute(cache, (c, ks) -> {
                if (ks == null) {
                    ks = new ArrayList<>();
                }
                ks.add(key);
                return ks;
            });
        }

        for (Map.Entry<Cache, List<String>> cacheEntry : shardingKeys.entrySet()) {
            Cache cache = cacheEntry.getKey();
            List<String> keysInOneShard = cacheEntry.getValue();
            remove = remove && cache.removeAll(region, keysInOneShard, collector);
        }
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        collector.collect(cacheActivelyEvictEvent);
        return remove;
    }

    @Override
    public boolean removeAll(String region, CacheEventCollector collector) {
        boolean re = true;
        if (!initialized) {
            return defaultCache.removeAll(region, collector);
        }
        for (Cache cache : shardingNodes.values()) {
            re = re & cache.removeAll(region, collector);
        }
        return re;
    }

    @Override
    public Entry wrap(String region, String key, byte[] value, long ttl) {
        return sharding(key).wrap(region, key, value, ttl);
    }

}
