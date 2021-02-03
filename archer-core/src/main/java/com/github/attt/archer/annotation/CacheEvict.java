package com.github.attt.archer.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static com.github.attt.archer.constants.Constants.DEFAULT_REGION;
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
public @interface CacheEvict {

    /**
     * 是否是要淘汰拆分后的缓存
     *
     * @return 默认是按照一个整体缓存
     * @see Cache#multi()
     */
    boolean multi() default false;

    /**
     * 缓存区域，用于隔离区分不同用途的缓存，也就是不同的region中的缓存
     * 即使缓存key一样也可以以多个缓存副本存在，这些缓存副本之间可以有
     * 不同的缓存状态
     * <p>
     * 淘汰指定缓存区域中所有的缓存
     *
     * @return 默认值空
     */
    String[] regions() default {DEFAULT_REGION};

    /**
     * 缓存key，可以直接指定常量，也可以使用SpringEL表达式使其在执行期间
     * 使用变量来生成key
     * <p>
     * 淘汰指定缓存key
     *
     * @return 默认值空
     */
    String[] keys() default {""};

    /**
     * 缓存key生成器，在spring环境中可以通过bean name指定，
     * 非spring环境中需要在启动配置中设置并实例化
     *
     * @see com.github.attt.archer.cache.KeyGenerator
     */
    String keyGenerator() default "";

    /**
     * 是否在方法执行完成后清空缓存
     *
     * @return 默认在执行方法前清空（true）
     */
    boolean afterInvocation() default false;

    /**
     * 缓存条件，使用SpringEL表达式在执行缓存流程时会先检查此表达式
     * 仅在表达式结果为true时执行缓存流程
     *
     * @return 默认为空（true）
     */
    String condition() default "";

    /**
     * 删除region中所有的缓存，此项设置为true会忽略keys的设置
     *
     * @return 默认false
     */
    boolean all() default false;
}
