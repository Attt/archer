package com.github.attt.archer.cache.caffeine;

import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.cache.api.CacheInitializer;
import com.github.attt.archer.cache.api.CacheShard;
import com.github.attt.archer.exception.CacheBeanParsingException;

/**
 * Internal caffeine based cache initializer
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public final class CaffeineCacheInitializer implements CacheInitializer {

    private static final int MAX_SHARD_COUNT = 1;

    private static int shardCount = 0;

    @Override
    public Cache initial(CacheShard cacheShard) throws Throwable {
        if (shardCount > MAX_SHARD_COUNT) {
            throw new CacheBeanParsingException("Exceed maximum caffeine sharding size " + MAX_SHARD_COUNT);
        }
        shardCount++;
        if (isCaffeineShard(cacheShard)) {

            CaffeineCache operation = new CaffeineCache();

            // init method should be invoked at last
            operation.init(cacheShard);
            return operation;
        }
        return null;
    }

    private boolean isCaffeineShard(CacheShard cacheShard) {
        return cacheShard instanceof CaffeineConfig;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }
}
