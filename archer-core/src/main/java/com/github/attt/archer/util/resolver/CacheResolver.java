package com.github.attt.archer.util.resolver;

import com.github.attt.archer.cache.annotation.Cache;
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
                chooseValue(classCacheMetadata.getArea(), annotation.region())
        );

        metadata.setInvokeAnyway(annotation.overwrite());
        metadata.setExpirationInMillis(annotation.expiration().expirationTimeUnit().toMillis(annotation.expiration().expiration()));
        metadata.setBreakdownProtect(annotation.protection().breakdownProtect());
        metadata.setBreakdownProtectTimeoutInMillis(annotation.protection().breakdownProtectTimeUnit().toMillis(annotation.protection().breakdownProtectTimeout()));
        metadata.setValueSerializer(chooseValue(classCacheMetadata.getValueSerializer(), annotation.valueSerializer()));
        metadata.setMultiple(!annotation.asOne());

        return Collections.singletonList(metadata);
    }
}
