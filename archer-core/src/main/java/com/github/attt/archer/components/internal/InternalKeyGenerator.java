package com.github.attt.archer.components.internal;

import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.components.preset.DefaultKeyGenerator;
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
