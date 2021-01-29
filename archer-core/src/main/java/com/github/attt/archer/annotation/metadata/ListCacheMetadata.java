package com.github.attt.archer.annotation.metadata;

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

    private String elementArea;

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


    public String getElementArea() {
        return elementArea;
    }

    public void setElementArea(String elementArea) {
        this.elementArea = elementArea;
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
                ", elementArea='" + elementArea + '\'' +
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
                ", area='" + area + '\'' +
                '}';
    }

    @Override
    public String initializedInfo() {
        return toString();
    }
}
