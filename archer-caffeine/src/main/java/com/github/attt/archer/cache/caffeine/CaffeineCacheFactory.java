package com.github.attt.archer.cache.caffeine;

import com.github.attt.archer.cache.Cache;
import com.github.attt.archer.cache.CacheFactory;
import com.github.attt.archer.cache.CacheConfig;
import com.github.attt.archer.exception.CacheBeanParsingException;

/**
 * Internal caffeine based cache initializer
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public final class CaffeineCacheFactory implements CacheFactory {

    private static final int MAX_SHARD_COUNT = 1;

    private static int shardCount = 0;

    @Override
    public Cache initial(CacheConfig config) throws Throwable {
        if (shardCount > MAX_SHARD_COUNT) {
            throw new CacheBeanParsingException("Exceed maximum caffeine sharding size " + MAX_SHARD_COUNT);
        }
        shardCount++;
        if (isCaffeineShard(config)) {

            CaffeineCache cache = new CaffeineCache();

            // init method should be invoked at last
            cache.init(config);
            return cache;
        }
        return null;
    }

    private boolean isCaffeineShard(CacheConfig cacheConfig) {
        return cacheConfig instanceof CaffeineConfig;
    }

    @Override
    public boolean enabled() {
        return true;
    }
}
