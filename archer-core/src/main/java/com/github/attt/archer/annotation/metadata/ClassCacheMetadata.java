package com.github.attt.archer.annotation.metadata;

import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;

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
