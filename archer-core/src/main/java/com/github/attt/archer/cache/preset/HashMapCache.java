package com.github.attt.archer.cache.preset;

import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.stats.api.CacheEventCollector;
import com.github.attt.archer.stats.event.CacheAccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hash map operation cache operation
 * Only for debugging
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class HashMapCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(HashMapCache.class);

    private final Map<String, Entry> unsafe = new WeakHashMap<>();

    private final Map<String, Entry> map = Collections.synchronizedMap(unsafe);

    private final Map<String, List<String>> areaKeysMap = new ConcurrentHashMap<>();

    @Override
    public boolean containsKey(String area, String key, CacheEventCollector collector) {
        boolean exists = map.containsKey(key);
        collector.collect(new CacheAccessEvent());
        return exists;
    }

    @Override
    public Entry get(String area, String key, CacheEventCollector collector) {
        Entry entry = map.get(key);
        collector.collect(new CacheAccessEvent());
        return entry;
    }

    @Override
    public Map<String, Entry> getAll(String area, Collection<String> keys, CacheEventCollector collector) {
        if (keys == null) {
            return new HashMap<>(0);
        }
        Map<String, Entry> result = new HashMap<>(keys.size());
        for (String key : keys) {
            result.put(key, map.getOrDefault(key, null));
            collector.collect(new CacheAccessEvent());
        }
        return result;
    }

    @Override
    public void put(String area, String key, Entry value, CacheEventCollector collector) {
        areaKeysMap.compute(area, (area1, keys) -> {
            if (keys == null) {
                keys = new ArrayList<>();
            }
            keys.add(key);
            return keys;
        });

        map.put(key, value);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putAll(String area, Map<String, Entry> map, CacheEventCollector collector) {
        areaKeysMap.compute(area, (area1, keys) -> {
            if (keys == null) {
                keys = new ArrayList<>();
            }
            keys.addAll(map.keySet());
            return keys;
        });
        this.map.putAll(map);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putIfAbsent(String area, String key, Entry value, CacheEventCollector collector) {
        areaKeysMap.compute(area, (area1, keys) -> {
            if (keys == null) {
                keys = new ArrayList<>();
            }
            keys.add(key);
            return keys;
        });
        map.putIfAbsent(key, value);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putAllIfAbsent(String area, Map<String, Entry> map, CacheEventCollector collector) {
        areaKeysMap.compute(area, (area1, keys) -> {
            if (keys == null) {
                keys = new ArrayList<>();
            }
            keys.addAll(map.keySet());
            return keys;
        });
        this.map.putAll(map);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public boolean remove(String area, String key, CacheEventCollector collector) {
        map.remove(key);
        areaKeysMap.compute(area, (area1, keys) -> {
            if (keys != null) {
                keys.remove(key);
            }
            return keys;
        });
        collector.collect(new CacheAccessEvent());
        return true;
    }

    @Override
    public boolean removeAll(String area, Collection<String> keys, CacheEventCollector collector) {
        for (String key : keys) {
            map.remove(key);
            areaKeysMap.compute(area, (area1, keys1) -> {
                if (keys1 != null) {
                    keys1.remove(key);
                }
                return keys1;
            });
            collector.collect(new CacheAccessEvent());
        }
        return false;
    }

    @Override
    public boolean removeAll(String area, CacheEventCollector collector) {
        List<String> keys = areaKeysMap.get(area);
        for (String key : keys) {
            map.remove(key);
        }
        return false;
    }

}
