package com.github.attt.archer.spring.config;


import com.github.attt.archer.cache.InternalCacheFactory;
import com.github.attt.archer.cache.CacheFactory;
import com.github.attt.archer.cache.CacheConfig;
import com.github.attt.archer.cache.ShardingCache;
import com.github.attt.archer.exception.CacheBeanParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Cache bean definition config
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@Configuration
public class ArcherCommonConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ArcherCommonConfiguration.class);

    /**
     * 初始化cache组件：
     * <ul>
     *     <li>CacheMetadata</li>
     *     <li>ValueSerializer</li>
     *     <li>KeyGenerator</li>
     *     <li>CacheProperties</li>
     * </ul>
     *
     * @return
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static CacheBeanDefinitionRegistryProcessor cacheBeanDefinitionRegistryProcessor() {
        return new CacheBeanDefinitionRegistryProcessor();
    }

    /**
     * 组件初始化完成后打印cache manager中的组件
     *
     * @return
     */
    @Bean(name = CacheComponentsPostPrintProcessor.CACHE_BEAN_FACTORY_PROCESSOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheComponentsPostPrintProcessor cacheComponentsPostPrintProcessor() {
        return new CacheComponentsPostPrintProcessor();
    }

    /**
     * 根据CacheConfig bean初始化ShardingCache
     *
     * @param cacheConfigs
     * @param cacheFactories
     * @return
     * @see CacheConfigBeanExistCondition
     * @see CachePropertiesAbsentCondition
     */
    @Bean(name = ShardingCache.SHARDING_CACHE_BEAN_NAME)
    @Conditional({CacheConfigBeanExistCondition.class, CachePropertiesAbsentCondition.class})
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public ShardingCache shardingCacheWithConfigBean(List<CacheConfig> cacheConfigs, List<CacheFactory> cacheFactories) {
        CacheFactory factory = cacheFactories.get(0);
        if (cacheFactories.size() > 1) {
            for (CacheFactory cacheFactory : cacheFactories) {
                if (!(cacheFactory instanceof InternalCacheFactory)) {
                    factory = cacheFactory;
                    break;
                }
            }
        }
        return new ShardingCache(factory, cacheConfigs);
    }

    /**
     * 根据properties中配置信息初始化ShardingCache
     *
     * @param cacheFactories
     * @param beanFactory
     * @return
     * @see CacheConfigBeanAbsentCondition
     * @see CachePropertiesExistCondition
     */
    @Bean(name = ShardingCache.SHARDING_CACHE_BEAN_NAME)
    @Conditional({CacheConfigBeanAbsentCondition.class, CachePropertiesExistCondition.class})
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public ShardingCache shardingCacheWithConfigProperties(List<CacheFactory> cacheFactories, ListableBeanFactory beanFactory) {
        CacheFactory factory = cacheFactories.get(0);
        if (cacheFactories.size() > 1) {
            for (CacheFactory cacheFactory : cacheFactories) {
                if (!(cacheFactory instanceof InternalCacheFactory)) {
                    factory = cacheFactory;
                    break;
                }
            }
        }
        try {
            Class<?> propertiesClass = Class.forName("com.github.attt.archer.spring.autoconfigure.properties.CacheProperties");
            Map<String, ?> beans = beanFactory.getBeansOfType(propertiesClass);
            if (beans.size() > 0) {
                Object properties = beans.values().iterator().next();
                Method getCacheConfigs = propertiesClass.getDeclaredMethod("getCacheConfigs");
                List<CacheConfig> cacheConfigs = (List) getCacheConfigs.invoke(properties);
                return new ShardingCache(factory, cacheConfigs);
            }
        } catch (Throwable t) {
            log.info("Error occurred when looking up com.github.attt.archer.spring.autoconfigure.properties.CacheProperties");
        }
        throw new CacheBeanParsingException("Failed to find com.github.attt.archer.spring.autoconfigure.properties.CacheProperties, plz check the dependency version of it.");
    }

    /**
     * 无CacheConfig初始化ShardingCache。
     * 用户没有配置或者装配CacheConfig的情况下，为了方便快速开箱会使用hashmap cache的默认实现
     *
     * @param cacheFactories
     * @return
     * @see CacheConfigBeanAbsentCondition
     * @see CachePropertiesAbsentCondition
     */
    @Bean(name = ShardingCache.SHARDING_CACHE_BEAN_NAME)
    @Conditional({CacheConfigBeanAbsentCondition.class, CachePropertiesAbsentCondition.class})
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public ShardingCache shardingCacheWithNoConfigBean(List<CacheFactory> cacheFactories) {
        CacheFactory factory = cacheFactories.get(0);
        if (cacheFactories.size() > 1) {
            for (CacheFactory cacheFactory : cacheFactories) {
                if (!(cacheFactory instanceof InternalCacheFactory)) {
                    factory = cacheFactory;
                    break;
                }
            }
        }
        return new ShardingCache(factory, null);
    }

    /**
     * 默认的cache factory
     *
     * @return
     * @see InternalCacheFactory
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheFactory cacheFactory() {
        return new InternalCacheFactory();
    }

    public static class CacheConfigBeanExistCondition implements Condition {

        @Override
        public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
            Map<String, CacheConfig> configMap = conditionContext.getBeanFactory().getBeansOfType(CacheConfig.class);
            return configMap.size() != 0;
        }
    }

    public static class CacheConfigBeanAbsentCondition implements Condition {

        private final CacheConfigBeanExistCondition cacheConfigBeanExistCondition = new CacheConfigBeanExistCondition();

        @Override
        public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
            return !cacheConfigBeanExistCondition.matches(conditionContext, annotatedTypeMetadata);
        }
    }

    public static class CachePropertiesExistCondition implements Condition {

        @Override
        public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
            try {
                Class<?> propertiesClass = Class.forName("com.github.attt.archer.spring.autoconfigure.properties.CacheProperties");
                Map propertiesMap = conditionContext.getBeanFactory().getBeansOfType(propertiesClass);
                return propertiesMap.size() != 0;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    public static class CachePropertiesAbsentCondition implements Condition {

        private final CachePropertiesExistCondition cachePropertiesExistCondition = new CachePropertiesExistCondition();

        @Override
        public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
            return !cachePropertiesExistCondition.matches(conditionContext, annotatedTypeMetadata);
        }
    }
}
