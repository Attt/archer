package com.github.attt.archer.annotation.metadata;

import com.github.attt.archer.roots.Component;

import java.lang.annotation.Annotation;

/**
 * Abstract cache metadata
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public abstract class AbstractCacheMetadata implements Component {

    protected Annotation cacheAnnotation;

    protected String methodSignature;

    protected String key;

    protected String keyPrefix;

    protected String condition;

    protected String keyGenerator;

    protected String region;

    public Annotation getCacheAnnotation() {
        return cacheAnnotation;
    }

    public void setCacheAnnotation(Annotation cacheAnnotation) {
        this.cacheAnnotation = cacheAnnotation;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getKeyGenerator() {
        return keyGenerator;
    }

    public void setKeyGenerator(String keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
