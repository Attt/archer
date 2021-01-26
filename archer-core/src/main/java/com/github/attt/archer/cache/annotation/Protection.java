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
public @interface Protection {

    /**
     * 开启缓存击穿保护
     */
    boolean breakdownProtect() default true;

    /**
     * 击穿保护的超时时间
     * 缓存击穿保护的原理是并发请求到来时，只分派其中一个请求执行任务，
     * 在任务执行完成后通知其余请求，此超时时间则是其余请求的等待超时时间，
     * 超过这个时间请求失效，返回失败
     *
     * @return 默认5
     */
    long breakdownProtectTimeout() default 5;

    /**
     * 击穿保护的超时时间单位
     *
     * @return 默认秒
     */
    TimeUnit breakdownProtectTimeUnit() default TimeUnit.SECONDS;

}
