package com.github.attt.archer.invocation;

import com.github.attt.archer.CacheManager;
import com.github.attt.archer.processor.api.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static com.github.attt.archer.constants.Constants.DEFAULT_REGION;


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

    /**
     * Evict cache with certain key
     *
     * @param key
     */
    public static void evict(String key) {
        Collection<AbstractProcessor> processors = cacheManager.getProcessors();
        for (AbstractProcessor processor : processors) {
            processor.delete(DEFAULT_REGION, key);
        }
    }

    /**
     * Evict cache with certain area and key
     *
     * @param area
     * @param key
     */
    public static void evict(String area, String key) {
        Collection<AbstractProcessor> processors = cacheManager.getProcessors();
        for (AbstractProcessor processor : processors) {
            processor.delete(area, key);
        }
    }

    /**
     * Evict all caches with certain area
     *
     * @param area
     */
    public static void evictAll(String area) {
        Collection<AbstractProcessor> processors = cacheManager.getProcessors();
        for (AbstractProcessor processor : processors) {
            processor.deleteAll(area);
        }
    }

    /**
     * If cache with specified key exists in default area
     *
     * @param area
     * @param key
     */
    public static boolean exist(String key) {
        Collection<AbstractProcessor> processors = cacheManager.getProcessors();
        for (AbstractProcessor processor : processors) {
            boolean exist = processor.exist(DEFAULT_REGION, key);
            if (exist) {
                return true;
            }
        }
        return false;
    }

    /**
     * If cache with specified area and key exists
     *
     * @param area
     * @param key
     */
    public static boolean exist(String area, String key) {
        Collection<AbstractProcessor> processors = cacheManager.getProcessors();
        for (AbstractProcessor processor : processors) {
            boolean exist = processor.exist(area, key);
            if (exist) {
                return true;
            }
        }
        return false;
    }
}
