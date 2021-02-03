package com.github.attt.archer.cache;

import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.roots.Component;
import com.github.attt.archer.util.ReflectionUtil;


/**
 * Internal cache factory
 * <p>
 * 内部缓存实现的缓存初始化工具的代理器
 * archer cache提供了两个默认缓存实现（除去hashmap这种），分别是redis和caffeine，
 * 同时引入这两种实现时，优先使用redis。
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class InternalCacheFactory implements CacheFactory, Component {

    private CacheFactory cacheFactory;

    private static final String INTERNAL_REDIS_INITIALIZER_CLASS = "com.github.attt.archer.cache.redis.RedisCacheFactory";
    private static final String INTERNAL_CAFFEINE_INITIALIZER_CLASS = "com.github.attt.archer.cache.caffeine.CaffeineCacheFactory";

    public InternalCacheFactory() {
        cacheFactory = registerInitializer(ReflectionUtil.findClass(INTERNAL_REDIS_INITIALIZER_CLASS, CacheFactory.class));
        if (cacheFactory == null) {
            cacheFactory = registerInitializer(ReflectionUtil.findClass(INTERNAL_CAFFEINE_INITIALIZER_CLASS, CacheFactory.class));
        }
    }

    @Override
    public Cache initial(CacheConfig config) throws Throwable {
        if (cacheFactory == null) {
            throw new CacheBeanParsingException("No internal cache implementation can be found. Plz provide some cache implementation.");
        }
        Cache cache = cacheFactory.initial(config);
        if (cache != null) {
            return cache;
        }
        throw new CacheBeanParsingException("Can't resolve cache shard config, present instance is " + config.getClass().getName());
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
}
