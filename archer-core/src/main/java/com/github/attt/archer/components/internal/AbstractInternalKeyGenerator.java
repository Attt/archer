package com.github.attt.archer.components.internal;

import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;
import com.github.attt.archer.util.CommonUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract internal key generator
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public abstract class AbstractInternalKeyGenerator implements KeyGenerator {

    /**
     * If any custom key generator is provided, use it before default key generator
     * but after the specified key generator of cache metadata
     */
    private KeyGenerator customKeyGenerator;

    private volatile boolean set = false;

    private Map<String, KeyGenerator> keyGeneratorMap = new HashMap<>();

    @Override
    public String generateKey(AbstractCacheMetadata metadata, Object target, Method method, Object[] args, Object result, Object resultElement) {
        if (!set) {
            synchronized (this) {
                if (!set) {
                    for (Map.Entry<String, KeyGenerator> entry : keyGeneratorMap.entrySet()) {
                        customKeyGenerator = entry.getValue();
                    }
                    set = true;
                }
            }
        }
        String metaKeyGeneratorName = getMetaKeyGeneratorName(metadata);

        if (!CommonUtils.isEmpty(metaKeyGeneratorName)) {
            try {
                KeyGenerator userKeyGenerator = keyGeneratorMap.get(metaKeyGeneratorName);
                return userKeyGenerator.generateKey(metadata, target, method, args, result, resultElement);
            } catch (Throwable ignored) {
            }
        }

        if (customKeyGenerator == null) {
            return defaultKeyGenerator().generateKey(metadata, target, method, args, result, resultElement);
        }
        return customKeyGenerator.generateKey(metadata, target, method, args, result, resultElement);
    }

    public void setKeyGeneratorMap(Map<String, KeyGenerator> keyGeneratorMap) {
        this.keyGeneratorMap = keyGeneratorMap;
    }

    public abstract String getMetaKeyGeneratorName(AbstractCacheMetadata metadata);

    public abstract KeyGenerator defaultKeyGenerator();
}
