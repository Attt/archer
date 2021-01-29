package com.github.attt.archer.spring.config;

import com.github.attt.archer.cache.Cache;
import com.github.attt.archer.cache.CacheFactory;
import com.github.attt.archer.cache.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Shard info config
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public final class SpringShardingCacheConfigure extends ShardingCacheConfigure implements InitializingBean, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SpringShardingCacheConfigure.class);

    private ApplicationContext applicationContext;


    public SpringShardingCacheConfigure(CacheFactory initializer, List<CacheConfig> shardList) {
        super(initializer, shardList);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    @Override
    public Cache sharding(String seed) {
        fallBackAutoConfiguredProperties();
        return super.sharding(seed);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void fallBackAutoConfiguredProperties() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    try {
                        Class<?> propertiesClass = Class.forName("com.github.attt.archer.spring.autoconfigure.properties.CacheProperties");
                        Map<String, ?> beans = applicationContext.getBeansOfType(propertiesClass);
                        if (beans.size() > 0) {
                            Object properties = beans.values().iterator().next();
                            Method toShardsInfo = propertiesClass.getDeclaredMethod("toShardsInfo");
                            this.shardList = (List) toShardsInfo.invoke(properties);
                        }
                        init();
                        initialized = true;
                    } catch (Exception ignored) {
                    }
                }
            }
        }

    }
}
