package com.github.attt.archer.cache.api;

import java.util.Iterator;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public interface ShardingConfigure {
    Cache sharding(String seed);

    Iterator<Cache> shards();
}
