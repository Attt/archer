package com.github.attt.archer.util.resolver;

import com.github.attt.archer.cache.annotation.CacheList;
import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.ListCacheMetadata;

import java.lang.reflect.Method;
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
                chooseValue(classCacheMetadata.getArea(), annotation.region())
        );

        metadata.setElementKey(annotation.element().key());
        metadata.setElementKeyGenerator(annotation.element().keyGenerator());
        metadata.setInvokeAnyway(annotation.overwrite());
        metadata.setElementValueSerializer(chooseValue(classCacheMetadata.getValueSerializer(), annotation.element().valueSerializer()));
        metadata.setExpirationInMillis(annotation.expiration().expirationTimeUnit().toMillis(annotation.expiration().expiration()));
        metadata.setBreakdownProtect(annotation.protection().breakdownProtect());
        metadata.setBreakdownProtectTimeoutInMillis(annotation.protection().breakdownProtectTimeUnit().toMillis(annotation.protection().breakdownProtectTimeout()));
        metadata.setElementArea(annotation.element().region());
        metadata.setElementExpirationInMillis(annotation.element().expiration().expirationTimeUnit().toMillis(annotation.element().expiration().expiration()));
        return Collections.singletonList(metadata);
    }
}
