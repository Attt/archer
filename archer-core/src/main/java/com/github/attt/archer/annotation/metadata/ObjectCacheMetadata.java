package com.github.attt.archer.annotation.metadata;

import com.github.attt.archer.annotation.Cache;

/**
 * {@link Cache} cache metadata
 *
 * @author atpexgo.wu
 * @see Cache
 * @since 1.0
 */
public class ObjectCacheMetadata extends CacheMetadata {

    private String valueSerializer;

    private boolean multiple;

    public String getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(String valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    public String toString() {
        return "ObjectCacheMetadata{" +
                "valueSerializer='" + valueSerializer + '\'' +
                ", multiple=" + multiple +
                ", expirationInMillis=" + expirationInMillis +
                ", breakdownProtect=" + breakdownProtect +
                ", breakdownProtectTimeoutInMillis=" + breakdownProtectTimeoutInMillis +
                ", invokeAnyway=" + invokeAnyway +
                ", cacheAnnotation=" + cacheAnnotation +
                ", methodSignature='" + methodSignature + '\'' +
                ", key='" + key + '\'' +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", condition='" + condition + '\'' +
                ", keyGenerator='" + keyGenerator + '\'' +
                '}';
    }

    @Override
    public String initializedInfo() {
        return toString();
    }
}
