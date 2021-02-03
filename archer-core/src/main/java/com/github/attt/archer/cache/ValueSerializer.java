package com.github.attt.archer.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Value serializer
 *
 * @param <V>
 * @author atpexgo.wu
 * @since 1.0
 */
public interface ValueSerializer<V> extends Serializer<V, byte[]> {

    Logger log = LoggerFactory.getLogger(ValueSerializer.class);

    /**
     * If deserialization failed, treat value as null
     *
     * @param source
     * @return
     */
    default V looseDeserialize(byte[] source) {
        try {
            return deserialize(source);
        } catch (Throwable t) {
            log.warn("Some of cached value deserialized failed, maybe you've changed the signature of cache object or you are now using different serialization ?");
        }
        return null;
    }
}
