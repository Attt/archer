package com.github.attt.archer.util.resolver;

import com.github.attt.archer.annotation.Evict;
import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.EvictionMetadata;

import java.lang.reflect.Method;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class EvictResolver implements AnnotationResolver<Evict, EvictionMetadata> {

    @Override
    public EvictionMetadata resolve(Method method, ClassCacheMetadata classCacheMetadata, Evict annotation) {
        EvictionMetadata metadata = new EvictionMetadata();
        resolveCommon(
                metadata,
                method,
                annotation,
                classCacheMetadata.getKeyPrefix(),
                annotation.key(),
                annotation.condition(),
                chooseValue(classCacheMetadata.getKeyGenerator(), annotation.keyGenerator()),
                chooseValue(classCacheMetadata.getArea(), annotation.area())
        );
        metadata.setAll(annotation.all());
        metadata.setAfterInvocation(annotation.afterInvocation());
        return metadata;
    }
}
