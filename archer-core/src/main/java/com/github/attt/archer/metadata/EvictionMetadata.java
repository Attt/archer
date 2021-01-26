package com.github.attt.archer.metadata;

import com.github.attt.archer.cache.annotation.Evict;
import com.github.attt.archer.metadata.api.AbstractCacheMetadata;

import java.util.Arrays;

/**
 * {@link Evict} annotation metadata
 *
 * @author atpexgo.wu
 * @see Evict
 * @since 1.0
 */
public class EvictionMetadata extends AbstractCacheMetadata {

    private Boolean afterInvocation;

    private Boolean multiple;

    private Boolean all;

    public Boolean getAfterInvocation() {
        return afterInvocation;
    }

    public void setAfterInvocation(Boolean afterInvocation) {
        this.afterInvocation = afterInvocation;
    }

    public Boolean getMultiple() {
        return multiple;
    }

    public void setMultiple(Boolean multiple) {
        this.multiple = multiple;
    }

    public Boolean getAll() {
        return all;
    }

    public void setAll(Boolean all) {
        this.all = all;
    }

    @Override
    public String toString() {
        return "EvictionMetadata{" +
                "afterInvocation=" + afterInvocation +
                ", multiple=" + multiple +
                ", all=" + all +
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
