package com.github.attt.archer.metadata;

import com.github.attt.archer.annotation.CacheList;

/**
 * {@link CacheList} cache metadata
 *
 * @author atpexgo.wu
 * @see CacheList
 * @since 1.0
 */
public class ListCacheMetadata extends CacheMetadata {

    private String elementValueSerializer;

    private String elementKey;

    private String elementKeyGenerator;

    private Long elementExpirationInMillis;

    public String getElementValueSerializer() {
        return elementValueSerializer;
    }

    public void setElementValueSerializer(String elementValueSerializer) {
        this.elementValueSerializer = elementValueSerializer;
    }

    public String getElementKey() {
        return elementKey;
    }

    public void setElementKey(String elementKey) {
        this.elementKey = elementKey;
    }

    public String getElementKeyGenerator() {
        return elementKeyGenerator;
    }

    public void setElementKeyGenerator(String elementKeyGenerator) {
        this.elementKeyGenerator = elementKeyGenerator;
    }

    public Long getElementExpirationInMillis() {
        return elementExpirationInMillis;
    }

    public void setElementExpirationInMillis(Long elementExpirationInMillis) {
        this.elementExpirationInMillis = elementExpirationInMillis;
    }

    @Override
    public String toString() {
        return "ListCacheMetadata{" +
                "elementValueSerializer='" + elementValueSerializer + '\'' +
                ", elementKey='" + elementKey + '\'' +
                ", elementKeyGenerator='" + elementKeyGenerator + '\'' +
                ", elementExpirationInMillis=" + elementExpirationInMillis +
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
