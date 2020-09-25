package com.github.attt.archer.cache.api;

import java.util.Iterator;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public interface ShardingConfigure {

    /**
     * Find {@link Cache} with sharding seed
     *
     * @param seed sharding seed
     * @return
     */
    Cache sharding(String seed);

    /**
     * Iterator for all {@link Cache}
     *
     * @return
     */
    Iterator<Cache> shards();
}
