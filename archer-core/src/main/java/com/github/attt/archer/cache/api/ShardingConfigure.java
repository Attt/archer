package com.github.attt.archer.cache.api;

import java.util.Iterator;

/**
 * Sharding configure
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface ShardingConfigure {

    /**
     * 根据seed查找分片 {@link Cache}
     *
     * @param seed 分片seed
     * @return seed对应的cache实例
     */
    Cache sharding(String seed);

    /**
     * 遍历所有分片缓存实例 {@link Cache}
     *
     * @return 所有分片缓存实例迭代器
     */
    Iterator<Cache> shards();
}
