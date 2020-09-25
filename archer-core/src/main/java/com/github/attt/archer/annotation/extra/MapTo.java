package com.github.attt.archer.annotation.extra;

import com.github.attt.archer.annotation.CacheMulti;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link CacheMulti} support annotation
 * <p>
 * Map parameter to result element field to
 * make handling noise element or absent element correctly. </br>
 * <p>
 * Noise element example: </br>
 * <ul>
 *     <li> result: {@code [User(id=1),User(id=2),User(id=3)]}
 *     <li> parameter: {@code [1,3]}
 * </ul>
 * noise element in this case is User(id=2) </br>
 * <p>
 * Absent element example: </br>
 * <ul>
 *     <li> result: {@code [User(id=1),User(id=3)]}
 *     <li> parameter: {@code [1,2,3]}
 * </ul>
 * absent element in this case is User(id=2) </br>
 * <p>
 * With this annotation, all parameters could be mapped to absolute result elements.
 *
 * @author atpexgo.wu
 * @see CacheMulti
 * @since 1.0
 */
@Documented
@Retention(RUNTIME)
@Target({PARAMETER})
public @interface MapTo {

    /**
     * Result element class field
     * Spring Expression Language (SpEL) expression is supported
     */
    String value();
}
