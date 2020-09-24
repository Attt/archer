package com.github.attt.archer.util.resolver;

import com.github.attt.archer.annotation.*;
import com.github.attt.archer.exception.CacheOperationException;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public enum CacheType {

    /**
     * @see Cache
     * @see CacheResolver
     */
    CACHE(Cache.class, new CacheResolver()),

    /**
     * @see CacheMulti
     * @see CacheMultiResolver
     */
    CACHE_MULTI(CacheMulti.class, new CacheMultiResolver()),

    /**
     * @see CacheList
     * @see CacheListResolver
     */
    CACHE_LIST(CacheList.class, new CacheListResolver()),

    /**
     * @see Evict
     * @see EvictResolver
     */
    EVICT(Evict.class, new EvictResolver()),

    /**
     * @see EvictMulti
     * @see EvictMultiResolver
     */
    EVICT_MULTI(EvictMulti.class, new EvictMultiResolver());

    private final Class<? extends Annotation> annotation;

    @SuppressWarnings("rawtypes")
    private final AnnotationResolver resolver;

    public AnnotationResolver getResolver() {
        return resolver;
    }

    @SuppressWarnings("rawtypes")
    CacheType(Class<? extends Annotation> annotation, AnnotationResolver resolver) {
        this.annotation = annotation;
        this.resolver = resolver;
    }

    private static final Map<Class<? extends Annotation>, CacheType> MAPPING = new HashMap<>(CacheType.values().length);

    static {
        for (CacheType cacheType : CacheType.values()) {
            MAPPING.put(cacheType.annotation, cacheType);
        }
    }

    public static CacheType resolve(Annotation annotation) {
        CacheType cacheType = null;
        if(annotation != null) {
            cacheType = MAPPING.get(annotation.annotationType());
        }
        if(cacheType == null){
            throw new CacheOperationException("No such cache type supported. " + (annotation == null ? "null" : annotation.annotationType().getName()));
        }
        return cacheType;
    }
}
