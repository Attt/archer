package com.github.attt.archer.spring.config;


import com.github.attt.archer.cache.CacheInitializerDelegate;
import com.github.attt.archer.cache.api.CacheInitializer;
import com.github.attt.archer.cache.api.CacheShard;
import com.github.attt.archer.cache.internal.ShardingCache;
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
    @Conditional(ShardInfoBeanExistCondition.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public SpringShardingCacheConfigure shardInfoConfiguration(List<CacheShard> shardList, List<CacheInitializer> cacheInitializers) {
        // todo
        CacheInitializer initializer = cacheInitializers.get(0);
        if (cacheInitializers.size() > 1) {
            for (CacheInitializer cacheInitializer : cacheInitializers) {
                if (!(cacheInitializer instanceof CacheInitializerDelegate)) {
                    initializer = cacheInitializer;
                    break;
                }
            }
        }
        return new SpringShardingCacheConfigure(initializer, shardList);
    }

    @Bean
    @Conditional(ShardInfoBeanAbsentCondition.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public SpringShardingCacheConfigure shardInfoConfiguration0(List<CacheInitializer> cacheInitializers) {
        CacheInitializer initializer = cacheInitializers.get(0);
        if (cacheInitializers.size() > 1) {
            for (CacheInitializer cacheInitializer : cacheInitializers) {
                if (!(cacheInitializer instanceof CacheInitializerDelegate)) {
                    initializer = cacheInitializer;
                    break;
                }
            }
        }
        return new SpringShardingCacheConfigure(initializer, new ArrayList<>());
    }


    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheInitializer cacheInitializer() {
        return new CacheInitializerDelegate();
    }

    public static class ShardInfoBeanAbsentCondition implements Condition {

        @Override
        public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
            Map<String, CacheShard> shards = conditionContext.getBeanFactory().getBeansOfType(CacheShard.class);
            return shards.size() == 0;
        }
    }

    public static class ShardInfoBeanExistCondition implements Condition {

        @Override
        public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
            Map<String, CacheShard> shards = conditionContext.getBeanFactory().getBeansOfType(CacheShard.class);
            return shards.size() != 0;
        }
    }
}
