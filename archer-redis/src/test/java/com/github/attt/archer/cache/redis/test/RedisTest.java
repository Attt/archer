package com.github.attt.archer.cache.redis.test;

import com.github.attt.archer.cache.Cache;
import com.github.attt.archer.cache.redis.RedisCache;
import com.github.attt.archer.cache.redis.RedisShard;
import com.github.attt.archer.components.internal.InternalKeySerializer;
import com.github.attt.archer.components.internal.InternalObjectValueSerializer;
import com.github.attt.archer.stats.api.CacheEvent;
import com.github.attt.archer.stats.api.CacheEventCollector;
import com.github.attt.archer.stats.api.listener.CacheStatsListener;
import org.junit.jupiter.api.Test;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class RedisTest {

    private static final CacheEventCollector dummyCollector = new CacheEventCollector() {
        @Override
        public void register(CacheStatsListener<CacheEvent> listener) {

        }

        @Override
        public void collect(CacheEvent cacheEvent) {

        }
    };

    private RedisCache initRedis() {
        RedisCache redisCache = new RedisCache();

        redisCache.setKeySerializer(new InternalKeySerializer());
        redisCache.setValueSerializer(new InternalObjectValueSerializer(Cache.DefaultEntry.class));

        redisCache.init(new RedisShard());
        return redisCache;
    }

    @Test
    public void testRemoveAll() {
        RedisCache redisCache = initRedis();
        redisCache.put("area1", "key1", new Cache.DefaultEntry("key1", "value1".getBytes(), -1L), dummyCollector);
        redisCache.put("area1", "key2", new Cache.DefaultEntry("key2", "value2".getBytes(), -1L), dummyCollector);

        System.out.println("gap");

        redisCache.removeAll("area1", dummyCollector);

        System.out.println("gap");
    }

    @Test
    public void testRemoveAllVeryEarliest() {
        RedisCache redisCache = initRedis();

        redisCache.removeAll("area1", dummyCollector);

        System.out.println("gap");
    }
}
