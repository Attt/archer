package com.github.attt.archer.annotation.config;

import com.github.attt.archer.annotation.metadata.EvictionMetadata;

/**
 * Cache eviction operation
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class EvictionConfig extends AbstractConfig<EvictionMetadata> {

    @Override
    public String toString() {
        return "EvictionOperation{" +
                "metadata=" + metadata +
                '}';
    }

    @Override
    public String initializedInfo() {
        return toString();
    }
}
