package com.github.attt.archer.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static com.github.attt.archer.constants.Constants.DEFAULT_REGION;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * 列表缓存
 * 列表缓存仅在结果结构为列表或数组时生效，整个缓存由两部分组成：
 * 列表索引和缓存实体，两部分缓存的配置可以分离。
 * 使用该缓存的目的是缓存实体可以被{@link Cache}缓存命中
 *
 * @author atpexgo
 * @since 1.0
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD, TYPE})
public @interface CacheList {

    /**
     * 缓存实体内容的配置
     *
     * @return 必填
     */
    Cache element();

    /**
     * 缓存区域，用于隔离区分不同用途的缓存，也就是不同的region中的缓存
     * 即使缓存key一样也可以以多个缓存副本存在，这些缓存副本之间可以有
     * 不同的缓存状态
     *
     * @return 默认值："r0"
     */
    String region() default DEFAULT_REGION;

    /**
     * 缓存key，可以直接指定常量，也可以使用SpringEL表达式使其在执行期间
     * 使用变量来生成key
     *
     * @return 必填
     */
    String key();

    /**
     * 过期策略
     *
     * @return 默认是一周（七天）
     */
    Expiration expiration() default @Expiration;

    /**
     * 保护策略
     *
     * @return 默认开启
     */
    Protection protection() default @Protection;

    /**
     * 缓存key生成器，在spring环境中可以通过bean name指定，
     * 非spring环境中需要在启动配置中设置并实例化
     *
     * @see com.github.attt.archer.components.api.KeyGenerator
     */
    String keyGenerator() default "";

    /**
     * 缓存条件，使用SpringEL表达式在执行缓存流程时会先检查此表达式
     * 仅在表达式结果为true时执行缓存流程
     *
     * @return 默认为空（true）
     */
    String condition() default "";

    /**
     * 覆盖缓存，无论缓存是否命中都会重新执行并将最新的结果缓存
     *
     * @return 默认false不缓存
     */
    boolean overwrite() default false;

}
