package com.github.attt.archer.util.resolver;

import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.api.AbstractCacheMetadata;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public interface AnnotationResolver<T extends Annotation, R extends AbstractCacheMetadata> {

    R resolve(Method method, ClassCacheMetadata classCacheMetadata, T annotation);

    default void resolveCommon(AbstractCacheMetadata metadata, Method method, T annotation, String keyPrefix, String key, String condition, String keyGenerator, String area) {
        metadata.setCacheAnnotation(annotation);
        metadata.setKeyPrefix(keyPrefix);
        metadata.setKey(key);
        metadata.setCondition(condition);
        metadata.setKeyGenerator(keyGenerator);
        metadata.setMethodSignature(ReflectionUtil.getSignature(method));
        metadata.setArea(area);
    }

    default String chooseValue(String global, String instant) {
        return CommonUtils.isEmpty(instant) ? global : instant;
    }
}
