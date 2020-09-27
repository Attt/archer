package com.github.attt.archer.util.resolver;

import com.github.attt.archer.annotation.Cache;
import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.ObjectCacheMetadata;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheResolver implements AnnotationResolver<Cache, List<ObjectCacheMetadata>> {

    @Override
    public List<ObjectCacheMetadata> resolve(Method method, ClassCacheMetadata classCacheMetadata, Cache annotation) {
        ObjectCacheMetadata metadata = new ObjectCacheMetadata();
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

        metadata.setInvokeAnyway(annotation.overwrite());
        metadata.setExpirationInMillis(annotation.expirationTimeUnit().toMillis(annotation.expiration()));
        metadata.setBreakdownProtect(annotation.breakdownProtect());
        metadata.setBreakdownProtectTimeoutInMillis(annotation.breakdownProtectTimeUnit().toMillis(annotation.breakdownProtectTimeout()));
        metadata.setValueSerializer(chooseValue(classCacheMetadata.getValueSerializer(), annotation.valueSerializer()));

        return Collections.singletonList(metadata);
    }
}
