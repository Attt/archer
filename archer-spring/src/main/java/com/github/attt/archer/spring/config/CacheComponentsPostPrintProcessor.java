package com.github.attt.archer.spring.config;

import com.github.attt.archer.CacheManager;
import com.github.attt.archer.util.InfoPrinter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Cache bean factory post processor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheComponentsPostPrintProcessor implements BeanFactoryPostProcessor {

    static final String CACHE_BEAN_FACTORY_PROCESSOR_BEAN_NAME = "archer.cacheComponentsPostPrintProcessor";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        CacheManager cacheManager = beanFactory.getBean(CacheManager.INTERNAL_CACHE_MANAGER_BEAN_NAME, CacheManager.class);
        InfoPrinter.printComponentUsageInfo(cacheManager);
    }
}
