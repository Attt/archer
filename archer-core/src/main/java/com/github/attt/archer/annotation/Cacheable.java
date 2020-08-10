package com.github.attt.archer.annotation;

import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.components.api.ValueSerializer;

import java.lang.annotation.*;

/**
 * Global properties annotation
 * <p>
 * Declare this annotation
 * to apply default cache properties for all methods in service using cache
 *
 * @author atpexgo.wu
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {

    /**
     * Cache area
     * <p>
     * Isolate caches
     */
    String area() default "";

    /**
     * Alias for {@link #prefix()}
     */
    String value() default "";

    /**
     * Cache key prefix
     */
    String prefix() default "";

    /**
     * Custom value serializer bean name
     *
     * @see ValueSerializer
     */
    String valueSerializer() default "";

    /**
     * Custom key generator bean name
     *
     * @see KeyGenerator
     */
    String keyGenerator() default "";
}
