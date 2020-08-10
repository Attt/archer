package com.github.attt.archer.cache.api;

import com.github.attt.archer.stats.api.CacheEventCollector;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Cache
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface Cache {

    /**
     * Initialize cache with {@link CacheShard}
     *
     * @param shard
     */
    default void init(CacheShard shard) {
    }

    /**
     * True if the specified key already cached
     *
     * @param area
     * @param key
     * @param collector
     * @return
     */
    boolean containsKey(String area, String key, CacheEventCollector collector);

    /**
     * Get entry cached with the specified key, return null if cache miss
     *
     * @param area
     * @param key
     * @param collector
     * @return
     */
    Entry get(String area, String key, CacheEventCollector collector);

    /**
     * Batch version for {@link #get(String, String, CacheEventCollector)}
     *
     * @param area
     * @param keys
     * @param collector
     * @return
     */
    Map<String, Entry> getAll(String area, Collection<String> keys, CacheEventCollector collector);

    /**
     * Cache entry with the specified key, <br>
     * replace the old one if already exists
     *
     * @param area
     * @param key
     * @param value
     * @param collector
     */
    void put(String area, String key, Entry value, CacheEventCollector collector);

    /**
     * Batch version for {@link #put(String, String, Entry, CacheEventCollector)}
     *
     * @param area
     * @param map
     * @param collector
     */
    void putAll(String area, Map<String, Entry> map, CacheEventCollector collector);

    /**
     * Cache entry with the specified key, <br>
     * keep the old one if already exists
     *
     * @param area
     * @param key
     * @param value
     * @param collector
     */
    void putIfAbsent(String area, String key, Entry value, CacheEventCollector collector);

    /**
     * Batch version for {@link #put(String, String, Entry, CacheEventCollector)}
     *
     * @param area
     * @param map
     * @param collector
     */
    void putAllIfAbsent(String area, Map<String, Entry> map, CacheEventCollector collector);

    /**
     * Remove entry with the specified key from cache
     *
     * @param area
     * @param key
     * @param collector
     * @return
     */
    boolean remove(String area, String key, CacheEventCollector collector);

    /**
     * Batch version for {@link #remove(String, String, CacheEventCollector)}
     *
     * @param area
     * @param keys
     * @param collector
     * @return
     */
    boolean removeAll(String area, Collection<String> keys, CacheEventCollector collector);

    /**
     * Remove all caches in area
     *
     * @param area
     * @param collector
     * @return
     */
    boolean removeAll(String area, CacheEventCollector collector);

    /**
     * Wrap key,value,ttl to Entry object
     *
     * @param key
     * @param value
     * @param ttl
     * @return
     */
    default Entry wrap(String area, String key, byte[] value, long ttl) {
        return new DefaultEntry(key, value, ttl);
    }

    interface Entry {

        String getKey();

        byte[] getValue();

        long getTtl();
    }

    class DefaultEntry implements Entry, Serializable {

        private String key;

        private byte[] value;

        private long ttl;

        public DefaultEntry(String key, byte[] value, long ttl) {
            this.key = key;
            this.value = value;
            this.ttl = ttl;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setValue(byte[] value) {
            this.value = value;
        }

        public void setTtl(long ttl) {
            this.ttl = ttl;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public byte[] getValue() {
            return value;
        }

        @Override
        public long getTtl() {
            return ttl;
        }
    }
}
