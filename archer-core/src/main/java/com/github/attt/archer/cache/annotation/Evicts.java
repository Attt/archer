package com.github.attt.archer.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link Evict} container
 *
 * @author atpexgo
 * @since 1.0
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Evicts {

    Evict[] value();
}
