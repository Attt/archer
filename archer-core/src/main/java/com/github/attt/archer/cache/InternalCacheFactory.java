package com.github.attt.archer.cache;

import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.roots.Component;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Internal cache operation initialization initializer delegate
 * <p>
 * 内部缓存实现的缓存初始化工具的代理器
 * archer cache提供了两个默认缓存实现（除去hashmap这种），分别是redis和caffeine，
 * 同时引入这两种实现时，根据优先级（priority）先使用redis。
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class InternalCacheFactory implements CacheFactory, Component {

    private CacheFactory cacheFactory;

    private static final String INTERNAL_REDIS_INITIALIZER_CLASS = "com.github.attt.archer.cache.redis.RedisCacheFactory";
    private static final String INTERNAL_CAFFEINE_INITIALIZER_CLASS = "com.github.attt.archer.cache.caffeine.CaffeineCacheFactory";

    public InternalCacheFactory() {
        cacheFactory = registerInitializer(findClass(INTERNAL_REDIS_INITIALIZER_CLASS));
        if (cacheFactory == null) {
            cacheFactory = registerInitializer(findClass(INTERNAL_CAFFEINE_INITIALIZER_CLASS));
        }
        if (cacheFactory == null) {
            throw new CacheBeanParsingException("No internal cache implementation can be found. Plz provide some cache implementation.");
        }
    }

    @Override
    public Cache initial(CacheConfig config) throws Throwable {
        Cache cache = cacheFactory.initial(config);
        if (cache != null) {
            return cache;
        }
        throw new CacheBeanParsingException("Can't resolve cache shard config, present instance is " + config.getClass().getName());
    }

    private Class<? extends CacheFactory> findClass(String name) {
        try {
            return (Class<? extends CacheFactory>) Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public CacheFactory registerInitializer(Class<? extends CacheFactory> cacheFactoryType) {

        if (cacheFactoryType != null) {
            try {
                CacheFactory cacheFactory = cacheFactoryType.newInstance();
                if (cacheFactory.enabled()) {
                    // double check before initializer added to delegate tree map
                    return cacheFactory;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public CacheFactory registerInitializer(CacheFactory cacheFactory) {
        if (cacheFactory != null) {
            if (cacheFactory.enabled()) {
                // double check before initializer added to delegate tree map
                return cacheFactory;
            }
        }
        return null;
    }
}
