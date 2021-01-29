package com.github.attt.archer.annotation.config;


import com.alibaba.fastjson.TypeReference;
import com.github.attt.archer.components.api.ValueSerializer;
import com.github.attt.archer.components.internal.InternalObjectValueSerializer;
import com.github.attt.archer.annotation.metadata.ListCacheMetadata;
import com.github.attt.archer.roots.ListComponent;

import java.util.Collection;
import java.util.List;

/**
 * Abstract listable cache operation
 *
 * @param <V> object cache value type
 * @author atpexgo.wu
 */
public class ListCacheConfig<V> extends AbstractCacheConfig<ListCacheMetadata, Collection<V>> implements ListComponent {

    private ValueSerializer<List<String>> elementCacheKeySerializer = new InternalObjectValueSerializer(new TypeReference<List<String>>() {
    }.getType());

    private ValueSerializer<V> valueSerializer;

    public ValueSerializer<List<String>> getElementCacheKeySerializer() {
        return elementCacheKeySerializer;
    }

    public void setElementCacheKeySerializer(ValueSerializer<List<String>> elementCacheKeySerializer) {
        this.elementCacheKeySerializer = elementCacheKeySerializer;
    }

    public ValueSerializer<V> getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(ValueSerializer<V> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }
}
