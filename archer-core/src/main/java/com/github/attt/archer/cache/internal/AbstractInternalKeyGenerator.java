package com.github.attt.archer.cache.internal;

import com.github.attt.archer.cache.KeyGenerator;
import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;
import com.github.attt.archer.util.CommonUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract internal key generator
 * 按照优先级依次断路调用不同声明方式的key生成器: <br>
 * <ul>
 *     <li>1. metadata key generator</li>
 *     <li>2. custom key generator</li>
 *     <li>3. default key generator</li>
 * </ul>
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public abstract class AbstractInternalKeyGenerator implements KeyGenerator {

    /**
     * 自定义key生成器
     */
    private KeyGenerator customKeyGenerator;

    private volatile boolean initial = false;

    private Map<String, KeyGenerator> keyGeneratorMap = new HashMap<>();

    @Override
    public String generateKey(AbstractCacheMetadata metadata, Object target, Method method, Object[] args, Object result, Object resultElement) {
        if (!initial) {
            synchronized (this) {
                if (!initial) {
                    for (Map.Entry<String, KeyGenerator> entry : keyGeneratorMap.entrySet()) {
                        customKeyGenerator = entry.getValue();
                    }
                    initial = true;
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
