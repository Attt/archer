package com.github.attt.archer.util.resolver;

import com.github.attt.archer.annotation.CacheMulti;
import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.ObjectCacheMetadata;

import java.lang.reflect.Method;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheMultiResolver implements AnnotationResolver<CacheMulti, ObjectCacheMetadata> {


    @Override
    public ObjectCacheMetadata resolve(Method method, ClassCacheMetadata classCacheMetadata, CacheMulti annotation) {
        ObjectCacheMetadata metadata = new ObjectCacheMetadata();
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

        metadata.setInvokeAnyway(annotation.overwrite());
        metadata.setExpirationInMillis(annotation.expirationTimeUnit().toMillis(annotation.expiration()));
        metadata.setBreakdownProtect(annotation.breakdownProtect());
        metadata.setBreakdownProtectTimeoutInMillis(annotation.breakdownProtectTimeUnit().toMillis(annotation.breakdownProtectTimeout()));
        metadata.setValueSerializer(chooseValue(classCacheMetadata.getValueSerializer(), annotation.valueSerializer()));
        metadata.setOrderBy(annotation.orderBy());
        metadata.setMultiple(true);

        return metadata;
    }
}
