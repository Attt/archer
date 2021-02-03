package com.github.attt.archer.cache.redis;

import com.github.attt.archer.cache.Cache;
import com.github.attt.archer.cache.CacheFactory;
import com.github.attt.archer.cache.CacheConfig;
import com.github.attt.archer.cache.internal.InternalKeySerializer;
import com.github.attt.archer.cache.internal.InternalObjectValueSerializer;

/**
 * Internal redis based cache initializer
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public final class RedisCacheFactory implements CacheFactory {

    @Override
    public Cache initial(CacheConfig config) throws Throwable {

        if (isRedisShard(config)) {

            RedisCache cache = new RedisCache();
            cache.setKeySerializer(new InternalKeySerializer());
            cache.setValueSerializer(new InternalObjectValueSerializer(Cache.DefaultEntry.class));

            // init method should be invoked at last
            cache.init(config);
            return cache;
        }
        return null;
    }

    private boolean isRedisShard(CacheConfig shard) {
        return shard instanceof RedisShard;
    }

    @Override
    public boolean enabled() {
        return true;
    }

}
