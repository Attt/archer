package com.github.attt.archer.spring.autoconfigure.properties;

import com.github.attt.archer.cache.CacheConfig;
import com.github.attt.archer.cache.caffeine.CaffeineConfig;
import com.github.attt.archer.cache.redis.RedisShard;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.attt.archer.cache.redis.Constant.DEFAULT_TIMEOUT;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
@ConfigurationProperties(prefix = "archer")
public class CacheProperties {

    private CaffeineConfig caffeine;

    private RedisConfig redis;

    public CaffeineConfig getCaffeine() {
        return caffeine;
    }

    public void setCaffeine(CaffeineConfig caffeine) {
        this.caffeine = caffeine;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public static class RedisConfig {

        List<RedisShard> shards;

        public List<RedisShard> getShards() {
            return shards;
        }

        public void setShards(List<RedisShard> shards) {
            this.shards = shards;
        }

        private long readTimeout = DEFAULT_TIMEOUT;

        private long connectTimeout = DEFAULT_TIMEOUT;

        public long getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
        }

        public long getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

    }

    public List<CacheConfig> toShardsInfo() {
        if (redis != null && !CollectionUtils.isEmpty(redis.shards)) {
            for (RedisShard shard : redis.shards) {
                if (shard.getReadTimeout() == DEFAULT_TIMEOUT && redis.readTimeout != DEFAULT_TIMEOUT) {
                    shard.setReadTimeout(redis.readTimeout);
                }
                if (shard.getConnectTimeout() == DEFAULT_TIMEOUT && redis.connectTimeout != DEFAULT_TIMEOUT) {
                    shard.setConnectTimeout(redis.connectTimeout);
                }
            }
            return new ArrayList<>(redis.shards);
        }
        if (caffeine != null) {
            return Collections.singletonList(caffeine);
        }
        return new ArrayList<>();
    }

}
