package com.github.attt.archer.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 多对象缓存映射，用于指定参数与返回结果的对应关系
 *
 * @author atpexgo
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface CacheMapping {

    /**
     * 填入当前参数对应的返回结果字段，空字符串表示直接映射到结果对象/值
     * 如：
     * <code>
     * public List&lt;User&gt; method0(@CacheMapping(toResult = "id") List&lt;Long&gt; userIds)
     * </code>
     * 指将参数的userIds与结果对象User中的id关联起来
     *
     *
     * @return 必填
     */
    String toResult() default "";

}
