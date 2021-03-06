package com.github.attt.archer.util.resolver;

import com.github.attt.archer.annotation.CacheList;
import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.ListCacheMetadata;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheListResolver implements AnnotationResolver<CacheList, List<ListCacheMetadata>> {

    @Override
    public List<ListCacheMetadata> resolve(Method method, ClassCacheMetadata classCacheMetadata, CacheList annotation) {
        ListCacheMetadata metadata = new ListCacheMetadata();
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

        metadata.setElementKey(annotation.elementKey());
        metadata.setElementKeyGenerator(annotation.elementKeyGenerator());
        metadata.setInvokeAnyway(annotation.overwrite());
        metadata.setElementValueSerializer(chooseValue(classCacheMetadata.getValueSerializer(), annotation.elementValueSerializer()));
        metadata.setExpirationInMillis(annotation.expirationTimeUnit().toMillis(annotation.expiration()));
        metadata.setBreakdownProtect(annotation.breakdownProtect());
        metadata.setBreakdownProtectTimeoutInMillis(annotation.breakdownProtectTimeUnit().toMillis(annotation.breakdownProtectTimeout()));
        metadata.setElementArea(annotation.elementArea());
        metadata.setElementExpirationInMillis(annotation.elementExpirationTimeUnit().toMillis(annotation.elementExpiration()));
        return Collections.singletonList(metadata);
    }
}
