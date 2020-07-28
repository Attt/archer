package com.atpex.archer.cache.api;

import com.atpex.archer.roots.Component;

/**
 * Cache operation initialization processor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface CacheInitializer extends Component {


    Cache initial(CacheShard shard) throws Throwable;

    default boolean enabled() {
        return true;
    }

    default int order() {
        return -1;
    }
}
