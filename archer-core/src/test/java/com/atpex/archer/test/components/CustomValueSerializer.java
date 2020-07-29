package com.atpex.archer.test.components;

import com.atpex.archer.components.api.ValueSerializer;
import com.atpex.archer.test.model.User;

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
