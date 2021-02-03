package com.github.attt.archer.spring.autoconfigure.annotation;

import com.github.attt.archer.cache.ValueSerializer;
import com.github.attt.archer.constants.Serialization;
import com.github.attt.archer.spring.autoconfigure.ActiveModeConfigSelector;
import com.github.attt.archer.spring.autoconfigure.ConfigSelector;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 * Enable archer cache framework
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ConfigSelector.class, ActiveModeConfigSelector.class})
public @interface EnableArcher {

    /**
     * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
     * to standard Java interface-based proxies. The default is {@code false}. <strong>
     * Applicable only if {@link #mode()} is set to {@link AdviceMode#PROXY}</strong>.
     * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
     * Spring-managed beans requiring proxying, not just those marked with {@code @Cacheable}.
     * For example, other beans marked with Spring's {@code @Transactional} annotation will
     * be upgraded to subclass proxying at the same time. This approach has no negative
     * impact in practice unless one is explicitly expecting one type of proxy vs another,
     * e.g. in tests.
     */
    boolean proxyTargetClass() default false;

    /**
     * Indicate how caching advice should be applied. The default is
     * {@link AdviceMode#PROXY}.
     *
     * @see AdviceMode
     */
    AdviceMode mode() default AdviceMode.PROXY;

    /**
     * Indicate the ordering of the execution of the caching advisor
     * when multiple advices are applied at a specific joinpoint.
     * The default is {@link Ordered#LOWEST_PRECEDENCE}.
     */
    int order() default Ordered.LOWEST_PRECEDENCE;

    String[] basePackages();

    /**
     * Custom valueSerializer bean name
     *
     * @see ValueSerializer
     */
    String valueSerializer() default "";

    /**
     * Enable cache metrics observing action
     */
    boolean enableMetrics() default true;

    /**
     * Internal value serialization
     */
    Serialization serialization() default Serialization.JAVA;
}
