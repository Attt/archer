package com.github.attt.archer.annotation;

import com.github.attt.archer.components.api.KeyGenerator;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Evict annotation
 *
 * @author atpexgo.wu
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Repeatable(EvictMultis.class)
public @interface EvictMulti {

    /**
     * Element cache area
     * <p>
     * Specify which area this cached elements stored in
     */
    String elementArea() default "";

    /**
     * Element cache key
     * <p>
     * Support Spring Expression Language (SpEL)
     */
    String elementKey();

    /**
     * Custom key generator name
     *
     * @see KeyGenerator
     */
    String keyGenerator() default "";

    /**
     * If evict cache `after` method invocation
     */
    boolean afterInvocation() default false;

    /**
     * Cache condition
     * <p>
     * Support Spring Expression Language (SpEL)
     */
    String condition() default "";
}
