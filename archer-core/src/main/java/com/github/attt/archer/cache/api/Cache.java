package com.github.attt.archer.cache.api;

import com.github.attt.archer.stats.api.CacheEventCollector;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import static com.github.attt.archer.constants.Constants.DEFAULT_KEYS_SUFFIX;

/**
 * Cache
 *
 * 定义了缓存的操作
 * 可以通过该接口支持不同的缓存中间件或者缓存实现。
 * 其中显示存在于操作方法参数中的{@link CacheEventCollector}是用来统一收集缓存行为，
 * 用于计算和统计缓存用量和指标。
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface Cache {

    /**
     * 初始化缓存{@link CacheShard}
     *
     * @param shard
     */
    default void init(CacheShard shard) {
    }

    /**
     * 指定的key是否命中缓存
     *
     * @param area region
     * @param key 缓存key
     * @param collector 事件收集器
     * @return
     */
    boolean containsKey(String area, String key, CacheEventCollector collector);

    /**
     * 获取缓存
     *
     * @param area region
     * @param key 缓存key
     * @param collector 事件收集器
     * @return 缓存内容，未命中为null
     */
    Entry get(String area, String key, CacheEventCollector collector);

    /**
     * 批量获取缓存
     *
     * @see #get(String, String, CacheEventCollector)
     * @param area region
     * @param keys 缓存key集合
     * @param collector 事件收集器
     * @return 缓存key 与 {@link Entry}包装过的缓存内容映射 ，忽略未命中的缓存
     */
    Map<String, Entry> getAll(String area, Collection<String> keys, CacheEventCollector collector);

    /**
     * 根据指定key缓存value，如果存在就替换
     *
     * @param area region
     * @param key 缓存key
     * @param value 缓存内容
     * @param collector 事件收集器
     */
    void put(String area, String key, Entry value, CacheEventCollector collector);

    /**
     * 批量缓存value
     *
     * @see #put(String, String, Entry, CacheEventCollector)
     * @param area region
     * @param map 缓存key与缓存内容的映射
     * @param collector 事件收集器
     */
    void putAll(String area, Map<String, Entry> map, CacheEventCollector collector);

    /**
     * 根据指定key缓存value，如果存在忽略
     *
     * @param area region
     * @param key 缓存key
     * @param value 缓存内容
     * @param collector 事件收集器
     */
    void putIfAbsent(String area, String key, Entry value, CacheEventCollector collector);

    /**
     * 批量缓存value
     *
     * @see #putIfAbsent(String, String, Entry, CacheEventCollector)
     * @param area region
     * @param map 缓存key与缓存内容的映射
     * @param collector 事件收集器
     */
    void putAllIfAbsent(String area, Map<String, Entry> map, CacheEventCollector collector);

    /**
     * 删除key对应的缓存内容
     *
     * @param area region
     * @param key 缓存key
     * @param collector 事件收集器
     * @return 是否成功删除
     */
    boolean remove(String area, String key, CacheEventCollector collector);

    /**
     * 批量删除key对应的缓存内容
     *
     * @param area region
     * @param keys 缓存key集合
     * @param collector 事件收集器
     * @return 是否成功删除
     */
    boolean removeAll(String area, Collection<String> keys, CacheEventCollector collector);

    /**
     * 批量删除area中所有的缓存内容
     *
     * @param area region
     * @param collector 事件收集器
     * @return 是否成功删除
     */
    boolean removeAll(String area, CacheEventCollector collector);

    /**
     * 保存region（原area）中所有的缓存key的集合或列表的key
     *
     * @param area region
     * @return 默认是region + ～keys
     */
    default String areaKeysSetKey(String area) {
        return area + DEFAULT_KEYS_SUFFIX;
    }


    /**
     * 用于包装缓存的region、key、value和ttl超时时间
     *
     * @param area region
     * @param key 缓存key
     * @param value 缓存内容
     * @param ttl 超时时间（毫秒）
     * @return 包装后的缓存内容
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

        /**
         * 超时时间，毫秒
         */
        private long ttl;

        public DefaultEntry() {
        }

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
