package com.github.attt.archer.cache;

import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;

import java.lang.reflect.Method;

/**
 * Key generator
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface KeyGenerator {

    /**
     * Generate cache key
     *
     * @param metadata cache metadata
     * @param target   the instance of proxied service
     * @param method   the method to be invoked
     * @param args     the args passed to the method
     * @param result   the result the method returned
     * @return string formed cache key
     */
    String generateKey(AbstractCacheMetadata metadata, Object target, Method method, Object[] args, Object result, Object resultElement);

}
