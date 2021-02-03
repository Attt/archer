package com.github.attt.archer.cache.preset;

import com.alibaba.fastjson.JSON;
import com.github.attt.archer.cache.ValueSerializer;

import java.lang.reflect.Type;

/**
 * Fast json object value serializer
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class FastJsonObjectSerializer<V> implements ValueSerializer<V> {

    private Type type;

    public FastJsonObjectSerializer() {
    }

    public FastJsonObjectSerializer(Type type) {
        this.type = type;
    }

    @Override
    public V deserialize(byte[] serialized) {
        if (type == null) {
            return JSON.parseObject(serialized, Object.class);
        }
        return JSON.parseObject(serialized, type);
    }

    @Override
    public byte[] serialize(V raw) {
        return JSON.toJSONBytes(raw);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
