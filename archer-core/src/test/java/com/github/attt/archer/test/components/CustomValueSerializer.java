package com.github.attt.archer.test.components;

import com.github.attt.archer.cache.ValueSerializer;
import com.github.attt.archer.test.model.User;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class CustomValueSerializer implements ValueSerializer<User> {

    @Override
    public User deserialize(byte[] serialized) {
        return null;
    }

    @Override
    public byte[] serialize(User raw) {
        return new byte[0];
    }
}
