package com.github.attt.archer.annotation.config;


import com.github.attt.archer.components.api.ValueSerializer;
import com.github.attt.archer.annotation.metadata.ObjectCacheMetadata;
import com.github.attt.archer.roots.ObjectComponent;

/**
 * Object cache operation
 *
 * @param <V> cache value type
 * @author atpexgo.wu
 * @since 1.0
 */
public class ObjectCacheConfig<V> extends AbstractCacheConfig<ObjectCacheMetadata, V> implements ObjectComponent {

    private ValueSerializer<V> valueSerializer;

    @Override
    public String toString() {
        return "ObjectCacheOperation{" +
                "valueSerializer=" + valueSerializer +
                ", metadata=" + metadata +
                '}';
    }

    public ValueSerializer<V> getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(ValueSerializer<V> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public String initializedInfo() {
        return toString();
    }
}
