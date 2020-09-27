package com.github.attt.archer.util.resolver;

import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.api.AbstractCacheMetadata;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public interface AnnotationResolver<T extends Annotation, R extends List<? extends AbstractCacheMetadata>> {

    /**
     * Resolve annotation to metadata
     *
     * @param method             annotated method
     * @param classCacheMetadata metadata of annotated class
     * @param annotation         annotation
     * @return cache metadata
     */
    R resolve(Method method, ClassCacheMetadata classCacheMetadata, T annotation);

    /**
     * Resolve common part of metadata
     *
     * @param metadata
     * @param method
     * @param annotation
     * @param keyPrefix
     * @param key
     * @param condition
     * @param keyGenerator
     * @param area
     */
    default void resolveCommon(AbstractCacheMetadata metadata, Method method, T annotation, String keyPrefix, String key, String condition, String keyGenerator, String area) {
        metadata.setCacheAnnotation(annotation);
        metadata.setKeyPrefix(keyPrefix);
        metadata.setKey(key);
        metadata.setCondition(condition);
        metadata.setKeyGenerator(keyGenerator);
        metadata.setMethodSignature(ReflectionUtil.getSignature(method));
        metadata.setArea(area);
    }

    /**
     * @param defaultValue
     * @param instant
     * @return
     */
    default String chooseValue(String defaultValue, String instant) {
        return CommonUtils.isEmpty(instant) ? defaultValue : instant;
    }
}
