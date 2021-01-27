package com.github.attt.archer.cache;

import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.cache.api.CacheInitializer;
import com.github.attt.archer.cache.api.CacheShard;
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
public class CacheInitializerDelegate implements CacheInitializer, Component {

    private final TreeMap<Integer, CacheInitializer> cacheInitializerTree;

    private static final String INTERNAL_REDIS_INITIALIZER_CLASS = "com.github.attt.archer.cache.redis.RedisCacheInitializer";
    private static final String INTERNAL_CAFFEINE_INITIALIZER_CLASS = "com.github.attt.archer.cache.caffeine.CaffeineCacheInitializer";

    public CacheInitializerDelegate() {
        cacheInitializerTree = new TreeMap<>();
        registerInitializer(findClass(INTERNAL_REDIS_INITIALIZER_CLASS));
        registerInitializer(findClass(INTERNAL_CAFFEINE_INITIALIZER_CLASS));
    }

    @Override
    public Cache initial(CacheShard shard) throws Throwable {
        InternalInitializerEntry initializerEntry = new InternalInitializerEntry();
        while ((initializerEntry = delegate(initializerEntry.next)) != null) {
            Cache cache = initializerEntry.initializer.initial(shard);
            if (cache != null) {
                return cache;
            }
        }
        throw new CacheBeanParsingException("Can't resolve cache shard config, present instance is " + shard.getClass().getName());
    }

    private InternalInitializerEntry delegate(int priority) {
        NavigableMap<Integer, CacheInitializer> map = cacheInitializerTree.tailMap(priority, true);
        Map.Entry<Integer, CacheInitializer> initializerEntry = map.firstEntry();
        if (initializerEntry == null) {
            return null;
        }
        InternalInitializerEntry entry = new InternalInitializerEntry();
        entry.initializer = initializerEntry.getValue();
        entry.next = priority + 1;
        if (!entry.initializer.enabled()) {
            // 初始化工具没有启用，尝试下一个初始化工具
            return delegate(entry.next);
        }
        return entry;
    }

    static class InternalInitializerEntry {
        int next;
        CacheInitializer initializer;
    }

    private Class<? extends CacheInitializer> findClass(String name) {
        try {
            return (Class<? extends CacheInitializer>) Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public void registerInitializer(Class<? extends CacheInitializer> cacheInitializerType) {
        if (cacheInitializerType == null) {
            return;
        }
        try {
            CacheInitializer initializer = cacheInitializerType.newInstance();
            if (initializer.enabled()) {
                // double check before initializer added to delegate tree map
                cacheInitializerTree.put(initializer.priority(), initializer);
            }
        } catch (Throwable ignored) {
        }

    }

    public void registerInitializer(CacheInitializer cacheInitializer) {
        if (cacheInitializer == null) {
            return;
        }
        if (cacheInitializer.enabled()) {
            // double check before initializer added to delegate tree map
            cacheInitializerTree.put(cacheInitializer.priority(), cacheInitializer);
        }
    }
}
