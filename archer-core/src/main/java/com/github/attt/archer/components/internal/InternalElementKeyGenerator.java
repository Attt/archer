package com.github.attt.archer.components.internal;

import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.components.preset.DefaultElementKeyGenerator;
import com.github.attt.archer.annotation.metadata.ListCacheMetadata;
import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;

/**
 * Internal element key generator
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class InternalElementKeyGenerator extends AbstractInternalKeyGenerator {

    private final DefaultElementKeyGenerator defaultKeyGenerator = new DefaultElementKeyGenerator();

    @Override
    public String getMetaKeyGeneratorName(AbstractCacheMetadata metadata) {
        ListCacheMetadata listableCacheMetadata = (ListCacheMetadata) metadata;
        return listableCacheMetadata.getElementKeyGenerator();
    }

    @Override
    public KeyGenerator defaultKeyGenerator() {
        return defaultKeyGenerator;
    }
}
