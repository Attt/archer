package com.github.attt.archer.components.preset;

import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;
import com.github.attt.archer.util.ReflectionUtil;

import java.lang.reflect.Method;

/**
 * Exclusive key generator
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class ExclusiveKeyGenerator implements KeyGenerator {

    @Override
    public String generateKey(AbstractCacheMetadata metadata, Object target, Method method, Object[] args, Object result, Object resultElement) {
        return ReflectionUtil.getSignatureWithArgValuesAndReturnType(method, args);
    }

}
