package com.github.attt.archer.util.resolver;

import com.github.attt.archer.annotation.EvictMulti;
import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.EvictionMetadata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class EvictMultiResolver implements AnnotationResolver<EvictMulti, List<EvictionMetadata>> {

    @Override
    public List<EvictionMetadata> resolve(Method method, ClassCacheMetadata classCacheMetadata, EvictMulti annotation) {
        List<EvictionMetadata> evictionMetadata = new ArrayList<>();

        String[] areas = new String[0];
        if (annotation.elementArea().length == 0) {
            if (classCacheMetadata.getArea() != null) {
                areas = new String[]{classCacheMetadata.getArea()};
            }
        } else {
            areas = annotation.elementArea();
        }

        for (String area : areas) {
            for (String key : annotation.elementKey()) {
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
                metadata.setMultiple(true);
                metadata.setAfterInvocation(annotation.afterInvocation());
            }
        }

        return evictionMetadata;
    }
}
