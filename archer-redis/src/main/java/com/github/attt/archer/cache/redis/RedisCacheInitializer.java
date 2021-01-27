package com.github.attt.archer.cache.redis;

import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.cache.api.CacheInitializer;
import com.github.attt.archer.cache.api.CacheShard;
import com.github.attt.archer.components.internal.InternalKeySerializer;
import com.github.attt.archer.components.internal.InternalObjectValueSerializer;

/**
 * Internal redis based cache initializer
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public final class RedisCacheInitializer implements CacheInitializer {

    @Override
    public Cache initial(CacheShard shard) throws Throwable {

        if (isRedisShard(shard)) {

            RedisCache operation = new RedisCache();
            operation.setKeySerializer(new InternalKeySerializer());
            operation.setValueSerializer(new InternalObjectValueSerializer(Cache.DefaultEntry.class));

            // init method should be invoked at last
            operation.init(shard);
            return operation;
        }
        return null;
    }

    private boolean isRedisShard(CacheShard shard) {
        return shard instanceof RedisShard;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public int priority() {
        return 0;
    }
}
