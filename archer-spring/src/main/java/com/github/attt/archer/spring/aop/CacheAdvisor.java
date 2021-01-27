package com.github.attt.archer.spring.aop;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    public static final String CACHE_ADVISOR_BEAN_NAME = "archer.internalCacheAdvisor";

    private String[] basePackages;

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public Pointcut getPointcut() {
        return new CachePointcut(basePackages);
    }

}
