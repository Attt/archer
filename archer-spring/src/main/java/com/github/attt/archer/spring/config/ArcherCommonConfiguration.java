package com.github.attt.archer.spring.config;


import com.github.attt.archer.cache.InternalCacheFactory;
import com.github.attt.archer.cache.CacheFactory;
import com.github.attt.archer.cache.CacheConfig;
import com.github.attt.archer.cache.ShardingCache;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.ArrayList;
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


    @Bean(name = CacheBeanFactoryProcessor.CACHE_BEAN_FACTORY_PROCESSOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheBeanFactoryProcessor beanFactoryProcessor() {
        return new CacheBeanFactoryProcessor();
    }


    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static CacheBeanDefinitionRegistryProcessor beanDefinitionRegistryProcessor() {
        return new CacheBeanDefinitionRegistryProcessor();
    }

    @Bean(name = ShardingCache.SHARDING_CACHE_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public ShardingCache shardingCacheOperationSource(SpringShardingCacheConfigure springShardingCacheConfigure) {
        return new ShardingCache(springShardingCacheConfigure);
    }

    @Bean
    @Conditional(CacheConfigBeanExistCondition.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public SpringShardingCacheConfigure shardInfoConfiguration(List<CacheConfig> shardList, List<CacheFactory> cacheFactories) {
        // todo
        CacheFactory initializer = cacheFactories.get(0);
        if (cacheFactories.size() > 1) {
            for (CacheFactory cacheFactory : cacheFactories) {
                if (!(cacheFactory instanceof InternalCacheFactory)) {
                    initializer = cacheFactory;
                    break;
                }
            }
        }
        return new SpringShardingCacheConfigure(initializer, shardList);
    }

    @Bean
    @Conditional(CacheConfigBeanAbsentCondition.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public SpringShardingCacheConfigure shardInfoConfiguration0(List<CacheFactory> cacheFactories) {
        CacheFactory initializer = cacheFactories.get(0);
        if (cacheFactories.size() > 1) {
            for (CacheFactory cacheFactory : cacheFactories) {
                if (!(cacheFactory instanceof InternalCacheFactory)) {
                    initializer = cacheFactory;
                    break;
                }
            }
        }
        return new SpringShardingCacheConfigure(initializer, new ArrayList<>());
    }


    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheFactory cacheInitializer() {
        return new InternalCacheFactory();
    }

    public static class CacheConfigBeanAbsentCondition implements Condition {

        @Override
        public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
            Map<String, CacheConfig> configMap = conditionContext.getBeanFactory().getBeansOfType(CacheConfig.class);
            return configMap.size() == 0;
        }
    }

    public static class CacheConfigBeanExistCondition implements Condition {

        @Override
        public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
            Map<String, CacheConfig> configMap = conditionContext.getBeanFactory().getBeansOfType(CacheConfig.class);
            return configMap.size() != 0;
        }
    }
}
