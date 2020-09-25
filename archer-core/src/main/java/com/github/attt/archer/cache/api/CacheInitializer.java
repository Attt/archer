package com.github.attt.archer.cache.api;

import com.github.attt.archer.roots.Component;

/**
 * Cache operation initialization processor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface CacheInitializer extends Component {

    /**
     * Initial {@link Cache} with {@link CacheShard}
     *
     * @param shard
     * @return
     * @throws Throwable
     */
    Cache initial(CacheShard shard) throws Throwable;

    /**
     * If initializer is enabled
     *
     * @return true if enabled
     */
    default boolean enabled() {
        return true;
    }

    /**
     * Initializing order
     *
     * @return order between {@link Integer#MIN_VALUE} to {@link Integer#MAX_VALUE}
     */
    default int order() {
        return -1;
    }
}
