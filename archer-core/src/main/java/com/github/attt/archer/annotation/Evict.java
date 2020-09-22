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
@Repeatable(Evicts.class)
public @interface Evict {

    /**
     * Cache area
     * <p>
     * Isolate caches. <br>
     * <p>
     * If area is not set, evict the caches keys are specified by {@link Evict#key()} or
     * {@link Evict#keyGenerator()}, But if this value is not empty,
     * then only the cache declaring this certain area will be evicted.
     */
    String area() default "";

    /**
     * Cache key
     * <p>
     * Support Spring Expression Language (SpEL)
     */
    String key();

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

    /**
     * To evict all entries in specified area
     * if area is set to empty, evict all entries in all areas
     */
    boolean all() default false;
}
