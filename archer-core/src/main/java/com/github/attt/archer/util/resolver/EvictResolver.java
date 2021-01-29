package com.github.attt.archer.util.resolver;

import com.github.attt.archer.annotation.CacheEvict;
import com.github.attt.archer.annotation.metadata.ClassCacheMetadata;
import com.github.attt.archer.annotation.metadata.EvictionMetadata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class EvictResolver implements AnnotationResolver<CacheEvict, List<EvictionMetadata>> {

    @Override
    public List<EvictionMetadata> resolve(Method method, ClassCacheMetadata classCacheMetadata, CacheEvict annotation) {
        List<EvictionMetadata> evictionMetadata = new ArrayList<>();

        String[] areas = new String[0];
        if (annotation.regions().length == 0) {
            if (classCacheMetadata.getArea() != null) {
                areas = new String[]{classCacheMetadata.getArea()};
            }
        } else {
            areas = annotation.regions();
        }

        for (String area : areas) {
            for (String key : annotation.keys()) {
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
