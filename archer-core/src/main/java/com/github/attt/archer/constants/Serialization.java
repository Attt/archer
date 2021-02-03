package com.github.attt.archer.constants;

import com.github.attt.archer.cache.preset.FastJsonObjectSerializer;
import com.github.attt.archer.cache.preset.HessianObjectSerializer;
import com.github.attt.archer.cache.preset.JavaObjectSerializer;
import com.github.attt.archer.cache.preset.KryoObjectSerializer;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public enum Serialization {

    /**
     * Alibaba fastjson
     *
     * @see FastJsonObjectSerializer
     */
    FAST_JSON("com.alibaba.fastjson.JSON"),

    /**
     * Kryo
     *
     * @see KryoObjectSerializer
     */
    KRYO("com.esotericsoftware.kryo.Kryo"),

    /**
     * Hessian
     *
     * @see HessianObjectSerializer
     */
    HESSIAN("com.caucho.hessian.io.HessianInput"),

    /**
     * Java
     *
     * @see JavaObjectSerializer
     */
    JAVA("java.lang.Object");

    private final String mainDependency;

    public String getMainDependency() {
        return mainDependency;
    }

    Serialization(String mainDependency) {
        this.mainDependency = mainDependency;
    }
}
