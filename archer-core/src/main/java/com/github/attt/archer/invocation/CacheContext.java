package com.github.attt.archer.invocation;

import com.github.attt.archer.CacheManager;
import com.github.attt.archer.processor.api.AbstractProcessor;
import com.github.attt.archer.roots.ListComponent;
import com.github.attt.archer.roots.ObjectComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.attt.archer.constants.Constants.DEFAULT_AREA;

/**
 * Service cacheable context
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@SuppressWarnings("all")
public class CacheContext {

    private static final Logger logger = LoggerFactory.getLogger(CacheContext.class);

    private static final ThreadLocal<Object> currentProxy = new ThreadLocal<>();

    private static CacheManager cacheManager;

    private CacheContext() {
    }

    public static void setCacheManager(CacheManager cacheManager) {
        if (CacheContext.cacheManager == null) {
            CacheContext.cacheManager = cacheManager;
        }
    }

    public static Object currentProxy() throws IllegalStateException {
        return currentProxy.get();
    }

    public static <S> S currentProxy(S s) throws IllegalStateException {
        return (S) currentProxy.get();
    }

    static Object setCurrentProxy(Object proxy) {
        Object old = currentProxy.get();
        if (proxy != null) {
            currentProxy.set(proxy);
        } else {
            currentProxy.remove();
        }
        return old;
    }

    public static void evictList(String key){
        AbstractProcessor processor = cacheManager.getProcessor(ListComponent.class);
        evict(DEFAULT_AREA, key, processor);
    }

    public static void evictObject(String key) {
        AbstractProcessor processor = cacheManager.getProcessor(ObjectComponent.class);
        evict(DEFAULT_AREA, key, processor);
    }

    public static void evictList(String area, String key) {
        AbstractProcessor processor = cacheManager.getProcessor(ListComponent.class);
        evict(area, key, processor);
    }

    public static void evictObject(String area, String key) {
        AbstractProcessor processor = cacheManager.getProcessor(ObjectComponent.class);
        evict(area, key, processor);
    }

    private static void evict(String area, String key, AbstractProcessor processor) {
        processor.deleteWithKey(area, key);
    }
}
