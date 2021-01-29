package com.github.attt.archer.interceptor;

import com.github.attt.archer.CacheManager;
import com.github.attt.archer.invoker.AbstractInvoker;
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
public class ManualCacheHandler {

    private static final Logger logger = LoggerFactory.getLogger(ManualCacheHandler.class);

    private static final ThreadLocal<Object> currentProxy = new ThreadLocal<>();

    private static CacheManager cacheManager;

    private ManualCacheHandler() {
    }

    public static void setCacheManager(CacheManager cacheManager) {
        if (ManualCacheHandler.cacheManager == null) {
            ManualCacheHandler.cacheManager = cacheManager;
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
        Collection<AbstractInvoker> processors = cacheManager.getProcessors();
        for (AbstractInvoker processor : processors) {
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
        Collection<AbstractInvoker> processors = cacheManager.getProcessors();
        for (AbstractInvoker processor : processors) {
            processor.delete(area, key);
        }
    }

    /**
     * Evict all caches with certain area
     *
     * @param area
     */
    public static void evictAll(String area) {
        Collection<AbstractInvoker> processors = cacheManager.getProcessors();
        for (AbstractInvoker processor : processors) {
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
        Collection<AbstractInvoker> processors = cacheManager.getProcessors();
        for (AbstractInvoker processor : processors) {
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
        Collection<AbstractInvoker> processors = cacheManager.getProcessors();
        for (AbstractInvoker processor : processors) {
            boolean exist = processor.exist(area, key);
            if (exist) {
                return true;
            }
        }
        return false;
    }
}
