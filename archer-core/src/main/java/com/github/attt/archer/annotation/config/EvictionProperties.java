package com.github.attt.archer.annotation.config;

import com.github.attt.archer.annotation.metadata.EvictionMetadata;

/**
 * Cache eviction operation
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class EvictionProperties extends AbstractProperties<EvictionMetadata> {

    @Override
    public String toString() {
        return "EvictionProperties{" +
                "metadata=" + metadata +
                '}';
    }

    @Override
    public String initializedInfo() {
        return toString();
    }
}
