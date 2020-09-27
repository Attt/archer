package com.github.attt.archer.util.resolver;

import com.github.attt.archer.annotation.Evict;
import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.EvictionMetadata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class EvictResolver implements AnnotationResolver<Evict, List<EvictionMetadata>> {

    @Override
    public List<EvictionMetadata> resolve(Method method, ClassCacheMetadata classCacheMetadata, Evict annotation) {
        List<EvictionMetadata> evictionMetadata = new ArrayList<>();

        String[] areas = new String[0];
        if (annotation.area().length == 0) {
            if (classCacheMetadata.getArea() != null) {
                areas = new String[]{classCacheMetadata.getArea()};
            }
        } else {
            areas = annotation.area();
        }

        for (String area : areas) {
            for (String key : annotation.key()) {
                EvictionMetadata metadata = new EvictionMetadata();
                resolveCommon(
                        metadata,
                        method,
                        annotation,
                        classCacheMetadata.getKeyPrefix(),
                        key,
                        annotation.condition(),
                        chooseValue(classCacheMetadata.getKeyGenerator(), annotation.keyGenerator()),
                        area
                );
                metadata.setAll(annotation.all());
                metadata.setAfterInvocation(annotation.afterInvocation());
                evictionMetadata.add(metadata);
            }
        }

        return evictionMetadata;
    }
}
