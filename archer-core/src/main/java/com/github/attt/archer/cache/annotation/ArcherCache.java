package com.github.attt.archer.cache.annotation;

import java.lang.annotation.*;

import static com.github.attt.archer.constants.Constants.DEFAULT_REGION;

/**
 * Archer缓存注解
 *
 * 此注解是可选注解，可以设置当前类下的所有方法缓存中的默认配置
 *
 * @author atpexgo.wu
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ArcherCache {

    /**
     * 缓存区域，用于隔离区分不同用途的缓存，也就是不同的region中的缓存
     * 即使缓存key一样也可以以多个缓存副本存在，这些缓存副本之间可以有
     * 不同的缓存状态
     *
     * 多个缓存可以声明同一个缓存region，相当于分组，快进到evict可以一次性
     * 淘汰多个缓存
     *
     * @return 默认值："r0"
     */
    String region() default DEFAULT_REGION;

    /**
     * 同 {@link #prefix()}
     */
    String value() default "";

    /**
     * 缓存key的通用前缀
     */
    String prefix() default "";

    /**
     * 缓存内容的序列化器，在spring环境中可以通过bean name指定，
     * 非spring环境中需要在启动配置中设置并实例化
     *
     * @see com.github.attt.archer.components.api.ValueSerializer
     */
    String valueSerializer() default "";

    /**
     * 缓存key生成器，在spring环境中可以通过bean name指定，
     * 非spring环境中需要在启动配置中设置并实例化
     *
     * @see com.github.attt.archer.components.api.KeyGenerator
     */
    String keyGenerator() default "";
}
