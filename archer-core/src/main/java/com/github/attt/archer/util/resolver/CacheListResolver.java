package com.github.attt.archer.util.resolver;

import com.github.attt.archer.annotation.CacheList;
import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.ListCacheMetadata;

import java.lang.reflect.Method;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheListResolver implements AnnotationResolver<CacheList, ListCacheMetadata> {

    @Override
    public ListCacheMetadata resolve(Method method, ClassCacheMetadata classCacheMetadata, CacheList annotation) {
        ListCacheMetadata metadata = new ListCacheMetadata();
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

        metadata.setElementKey(annotation.elementKey());
        metadata.setElementKeyGenerator(annotation.elementKeyGenerator());
        metadata.setInvokeAnyway(annotation.overwrite());
        metadata.setElementValueSerializer(chooseValue(classCacheMetadata.getValueSerializer(), annotation.elementValueSerializer()));
        metadata.setExpirationInMillis(annotation.expirationTimeUnit().toMillis(annotation.expiration()));
        metadata.setBreakdownProtect(annotation.breakdownProtect());
        metadata.setBreakdownProtectTimeoutInMillis(annotation.breakdownProtectTimeUnit().toMillis(annotation.breakdownProtectTimeout()));
        metadata.setElementArea(annotation.elementArea());
        return metadata;
    }
}
