package com.github.attt.archer.metadata;

import com.github.attt.archer.metadata.api.AbstractCacheMetadata;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class ClassCacheMetadata extends AbstractCacheMetadata {

    private String valueSerializer;

    public String getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(String valueSerializer) {
        this.valueSerializer = valueSerializer;
    }
}
