package com.github.attt.archer.cache.redis;

import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.cache.api.CacheShard;
import com.github.attt.archer.components.api.Serializer;
import com.github.attt.archer.components.api.ValueSerializer;
import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.stats.api.CacheEventCollector;
import com.github.attt.archer.stats.event.CacheAccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Redis cache operation
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public final class RedisCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);

    private JedisPool jedisPool;

    private Serializer<String, byte[]> keySerializer;

    @SuppressWarnings("all")
    private ValueSerializer valueSerializer;

    public void setKeySerializer(Serializer<String, byte[]> keySerializer) {
        this.keySerializer = keySerializer;
    }

    public void setValueSerializer(@SuppressWarnings("all") ValueSerializer valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public void init(CacheShard shard) {
        if (!(shard instanceof RedisShard)) {
            throw new CacheBeanParsingException("Cache operation shard info supplied is not a instance of RedisShard");
        }
        RedisShard redisShard = (RedisShard) shard;

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setLifo(redisShard.isLifo());
        jedisPoolConfig.setFairness(redisShard.isFairness());
        jedisPoolConfig.setMaxTotal(redisShard.getMaxTotal());
        jedisPoolConfig.setMaxIdle(redisShard.getMaxIdle());
        jedisPoolConfig.setMinIdle(redisShard.getMinIdle());
        jedisPoolConfig.setMaxWaitMillis(redisShard.getMaxWaitMillis());
        jedisPoolConfig.setMinEvictableIdleTimeMillis(redisShard.getMinEvictableIdleTimeMillis());
        jedisPoolConfig.setSoftMinEvictableIdleTimeMillis(redisShard.getSoftMinEvictableIdleTimeMillis());
        jedisPoolConfig.setNumTestsPerEvictionRun(redisShard.getNumTestsPerEvictionRun());
        jedisPoolConfig.setTestOnCreate(redisShard.isTestOnCreate());
        jedisPoolConfig.setTestOnBorrow(redisShard.isTestOnBorrow());
        jedisPoolConfig.setTestOnReturn(redisShard.isTestOnReturn());
        jedisPoolConfig.setTestWhileIdle(redisShard.isTestWhileIdle());
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(redisShard.getTimeBetweenEvictionRunsMillis());
        jedisPoolConfig.setBlockWhenExhausted(redisShard.isBlockWhenExhausted());

        if (redisShard.getPassword() == null || "".equals(redisShard.getPassword())) {
            jedisPool = new JedisPool(jedisPoolConfig, redisShard.getHost(), redisShard.getPort(), (int) redisShard.getConnectTimeout(), null, redisShard.getDatabase(), redisShard.isSsl());
        } else {
            jedisPool = new JedisPool(jedisPoolConfig, redisShard.getHost(), redisShard.getPort(), (int) redisShard.getConnectTimeout(), redisShard.getPassword(), redisShard.getDatabase(), redisShard.isSsl());
        }
    }

    @Override
    public boolean containsKey(String area, String key, CacheEventCollector collector) {
        return autoClose(jedis -> {
            Boolean hasKey = jedis.exists(key);
            collector.collect(new CacheAccessEvent());
            return hasKey != null && hasKey;
        });
    }

    private Jedis jedis() {
        return jedisPool.getResource();
    }

    @Override
    public Entry get(String area, String key, CacheEventCollector collector) {

        return autoClose(jedis -> {
            byte[] value = jedis.get(keySerializer.serialize(key));
            collector.collect(new CacheAccessEvent());
            if (value == null) {
                return null;
            }
            return (Entry) valueSerializer.looseDeserialize(value);
        });
    }

    @Override
    public Map<String, Entry> getAll(String area, Collection<String> keys, CacheEventCollector collector) {
        return autoClose(jedis -> {
            Map<String, Entry> entryMap = new HashMap<>();
            List<byte[]> values = jedis.mget(keys.stream().map(k -> keySerializer.serialize(k)).toArray(byte[][]::new));
            collector.collect(new CacheAccessEvent());
            String[] keysArray = keys.toArray(new String[0]);
            for (int i = 0; i < keysArray.length; i++) {
                byte[] valueBytes = values.get(i);
                if (valueBytes == null) {
                    entryMap.put(keysArray[i], null);
                } else {
                    entryMap.put(keysArray[i], (Entry) valueSerializer.looseDeserialize(valueBytes));
                }
            }
            return entryMap;
        });
    }

    @Override
    public void put(String area, String key, Entry value, CacheEventCollector collector) {
        execute(putOp(area, key, value));
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putAll(String area, Map<String, Entry> map, CacheEventCollector collector) {
        execute(putAllOp(area, map));
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putIfAbsent(String area, String key, Entry value, CacheEventCollector collector) {
        execute(putIfAbsentOp(area, key, value));
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putAllIfAbsent(String area, Map<String, Entry> map, CacheEventCollector collector) {
        execute(putAllIfAbsentOp(area, map));
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public boolean remove(String area, String key, CacheEventCollector collector) {
        Boolean succeed = autoClose(jedis -> {
            Long delCnt = jedis.del(keySerializer.serialize(key));
            /*
                Remove cache key of ~keys set after real cache is removed
                to prevent consistent issue
             */
            jedis.srem(keySerializer.serialize(areaKeysSetKey(area)), keySerializer.serialize(key));
            return delCnt != 0;
        });
        collector.collect(new CacheAccessEvent());
        return succeed;
    }

    @Override
    public boolean removeAll(String area, Collection<String> keys, CacheEventCollector collector) {
        List<byte[]> keysInBytes = new ArrayList<>();
        for (String key : keys) {
            collector.collect(new CacheAccessEvent());
            keysInBytes.add(keySerializer.serialize(key));
        }
        Boolean succeed = autoClose(jedis -> {
            Long delCnt = jedis.del(keysInBytes.toArray(new byte[0][]));
            /*
                Remove cache key of ~keys set after real cache is removed
                to prevent consistent issue
             */
            jedis.srem(keySerializer.serialize(areaKeysSetKey(area)), keysInBytes.toArray(new byte[0][]));
            return delCnt != 0;
        });
        collector.collect(new CacheAccessEvent());
        return succeed;
    }

    @Override
    public boolean removeAll(String area, CacheEventCollector collector) {
        autoClose(jedis -> {
            // Use lua script to ensure that no other OP. will be fired when deleting area keys
            Long r = (Long) jedis.eval(
                    keySerializer.serialize("local size = redis.call('SCARD', KEYS[1]); if size > 0 then local keys = redis.call('SMEMBERS', KEYS[1]); for _, k in ipairs(keys) do redis.call('DEL', k); redis.call('SREM', KEYS[1], k); end return 1; end return 0;"),
                    1,
                    keySerializer.serialize(areaKeysSetKey(area)));
            return r != 0;
        });
        return false;
    }

    private <R> R autoClose(Function<Jedis, R> function) {
        try (Jedis jedis = jedis()) {
            return function.apply(jedis);
        }
    }

    private void execute(Consumer<Pipeline> redisCallback) {
        autoClose(jedis -> {
            Pipeline pipelined = jedis.pipelined();
            redisCallback.accept(pipelined);
            pipelined.sync();
            return null;
        });
    }

    @SuppressWarnings("all")
    private Consumer<Pipeline> putOp(String area, String key, Entry value) {
        return pipeline -> {
            /*
                Push to ~keys set first to help resolving consistant issue.
                It'll cause deleting an extra absent cache when remove all caches in area
                if unfortunately this step is succeed but setting cache is failed.
             */
            pipeline.sadd(keySerializer.serialize(areaKeysSetKey(area)), keySerializer.serialize(key));
            pipeline.set(keySerializer.serialize(key), (byte[]) valueSerializer.serialize(value));
            if (value.getTtl() != -1L) {
                pipeline.pexpire(keySerializer.serialize(key), value.getTtl());
            }
        };
    }

    @SuppressWarnings("all")
    private Consumer<Pipeline> putAllOp(String area, Map<String, Entry> stringMap) {
        return pipeline -> {
            /*
                Push to ~keys set first to help resolving consistant issue.
                It'll cause deleting an extra absent cache when remove all caches in area
                if unfortunately this step is succeed but setting cache is failed.
            */
            byte[][] keyBytesArray = stringMap.entrySet().stream().map(kv -> keySerializer.serialize(kv.getKey())).collect(Collectors.toList()).toArray(new byte[0][]);
            pipeline.sadd(keySerializer.serialize(areaKeysSetKey(area)), keyBytesArray);
            for (Map.Entry<String, Entry> kv : stringMap.entrySet()) {
                pipeline.set(keySerializer.serialize(kv.getKey()), (byte[]) valueSerializer.serialize(kv.getValue()));
                if (kv.getValue().getTtl() != -1L) {
                    pipeline.pexpire(keySerializer.serialize(kv.getKey()), kv.getValue().getTtl());
                }
            }
        };
    }

    @SuppressWarnings("all")
    private Consumer<Pipeline> putAllIfAbsentOp(String area, Map<String, Entry> stringMap) {
        return pipeline -> {
            /*
                Push to ~keys set first to help resolving consistant issue.
                It'll cause deleting an extra absent cache when remove all caches in area
                if unfortunately this step is succeed but setting cache is failed.
            */
            byte[][] keyBytesArray = stringMap.entrySet().stream().map(kv -> keySerializer.serialize(kv.getKey())).collect(Collectors.toList()).toArray(new byte[0][]);
            pipeline.sadd(keySerializer.serialize(areaKeysSetKey(area)), keyBytesArray);
            for (Map.Entry<String, Entry> kv : stringMap.entrySet()) {
                pipeline.setnx(keySerializer.serialize(kv.getKey()), (byte[]) valueSerializer.serialize(kv.getValue()));
                if (kv.getValue().getTtl() != -1L) {
                    pipeline.pexpire(keySerializer.serialize(kv.getKey()), kv.getValue().getTtl());
                }
            }
        };
    }


    @SuppressWarnings("all")
    private Consumer<Pipeline> putIfAbsentOp(String area, String key, Entry value) {
        return pipeline -> {
            /*
                Push to ~keys set first to help resolving consistant issue.
                It'll cause deleting an extra absent cache when remove all caches in area
                if unfortunately this step is succeed but setting cache is failed.
             */
            pipeline.sadd(keySerializer.serialize(areaKeysSetKey(area)), keySerializer.serialize(key));
            pipeline.setnx(keySerializer.serialize(key), (byte[]) valueSerializer.serialize(value));
            if (value.getTtl() != -1L) {
                pipeline.pexpire(keySerializer.serialize(key), value.getTtl());
            }
        };
    }

}
