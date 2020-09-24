package com.github.attt.archer.components.preset;

import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.exception.CacheOperationException;
import com.github.attt.archer.metadata.api.AbstractCacheMetadata;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.SpringElUtil;

import java.lang.reflect.Method;

import static com.github.attt.archer.constants.Constants.DEFAULT_DELIMITER;

/**
 * Default key generator
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class DefaultKeyGenerator implements KeyGenerator {

    @Override
    public String generateKey(AbstractCacheMetadata metadata, Object target, Method method, Object[] args, Object result, Object resultElement) {

        String area = metadata.getArea();
        String prefix = metadata.getKeyPrefix();
        String key = metadata.getKey();

        if (CommonUtils.isEmpty(key)) {
            throw new CacheOperationException("No cache key provided!");
        }
        Object value = SpringElUtil.parse(key).setMethodInvocationContext(target, method, args, result).addVar("result$each", resultElement).getValue();
        String cacheKey = String.valueOf(value);

        String prefixedCacheKey = CommonUtils.isEmpty(prefix) ? cacheKey : prefix + DEFAULT_DELIMITER + cacheKey;
        return CommonUtils.isEmpty(area) ? prefixedCacheKey : area + DEFAULT_DELIMITER + prefixedCacheKey;
    }

}
