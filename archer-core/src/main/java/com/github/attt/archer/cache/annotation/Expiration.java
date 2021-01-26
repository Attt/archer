package com.github.attt.archer.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author atpexgo
 */
@Documented
@Retention(RUNTIME)
public @interface Expiration {

    /**
     * 缓存过期时间
     *
     * @return 默认3600 * 24 * 7
     */
    long expiration() default 3600 * 24 * 7;

    /**
     * 缓存过期时间单位
     *
     * @return 默认秒
     */
    TimeUnit expirationTimeUnit() default TimeUnit.SECONDS;

}
