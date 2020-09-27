package com.github.attt.archer.operation;

import com.github.attt.archer.metadata.EvictionMetadata;
import com.github.attt.archer.operation.api.AbstractOperation;

/**
 * Cache eviction operation
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class EvictionOperation extends AbstractOperation<EvictionMetadata> {

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
