package com.github.attt.archer.util.resolver;

import com.github.attt.archer.annotation.EvictMulti;
import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.EvictionMetadata;

import java.lang.reflect.Method;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class EvictMultiResolver implements AnnotationResolver<EvictMulti, EvictionMetadata> {

    @Override
    public EvictionMetadata resolve(Method method, ClassCacheMetadata classCacheMetadata, EvictMulti annotation) {
        EvictionMetadata metadata = new EvictionMetadata();
        resolveCommon(
                metadata,
                method,
                annotation,
                classCacheMetadata.getKeyPrefix(),
                annotation.elementKey(),
                annotation.condition(),
                chooseValue(classCacheMetadata.getKeyGenerator(), annotation.keyGenerator()),
                chooseValue(classCacheMetadata.getArea(), annotation.elementArea())
        );
        metadata.setMultiple(true);
        metadata.setAfterInvocation(annotation.afterInvocation());
        return metadata;
    }
}
