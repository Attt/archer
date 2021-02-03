package com.github.attt.archer.cache.internal;

import com.github.attt.archer.cache.KeyGenerator;
import com.github.attt.archer.cache.preset.DefaultKeyGenerator;
import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;

/**
 * Internal key generator
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class InternalKeyGenerator extends AbstractInternalKeyGenerator {

    private final DefaultKeyGenerator defaultKeyGenerator = new DefaultKeyGenerator();

    @Override
    public String getMetaKeyGeneratorName(AbstractCacheMetadata metadata) {
        return metadata.getKeyGenerator();
    }

    @Override
    public KeyGenerator defaultKeyGenerator() {
        return defaultKeyGenerator;
    }
}
