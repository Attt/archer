package com.github.attt.archer.annotation;

import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.components.api.ValueSerializer;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Cache List annotation
 * <p>
 * Used for batch querying method with one certain key
 *
 * @author atpexgo
 * @since 1.0
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD, TYPE})
public @interface CacheList {

    /**
     * Cache key
     * <p>
     * Support Spring Expression Language (SpEL)
     */
    String key();

    /**
     * Element cache key
     * <p>
     * Support Spring Expression Language (SpEL)
     */
    String elementKey();

    /**
     * Expiration
     */
    long expiration() default 3600 * 24 * 7;

    /**
     * Expiration time unit
     */
    TimeUnit expirationTimeUnit() default TimeUnit.SECONDS;

    /**
     * Expiration for element
     */
    long elementExpiration() default 3600 * 24 * 7;

    /**
     * Expiration time unit for element
     */
    TimeUnit elementExpirationTimeUnit() default TimeUnit.SECONDS;

    /**
     * Enable cache breakdown protect
     */
    boolean breakdownProtect() default true;

    /**
     * Breakdown protect timeout
     */
    long breakdownProtectTimeout() default 5;

    /**
     * Breakdown protect timeout time unit
     */
    TimeUnit breakdownProtectTimeUnit() default TimeUnit.SECONDS;

    /**
     * Custom element value serializer name
     *
     * @see ValueSerializer
     */
    String elementValueSerializer() default "";

    /**
     * Custom key generator name
     *
     * @see KeyGenerator
     */
    String keyGenerator() default "";

    /**
     * Custom element key generator name
     *
     * @see KeyGenerator
     */
    String elementKeyGenerator() default "";

    /**
     * Cache condition
     * <p>
     * Support Spring Expression Language (SpEL)
     */
    String condition() default "";

    /**
     * Overwrite no matter cache exists or not
     */
    boolean overwrite() default false;
}
