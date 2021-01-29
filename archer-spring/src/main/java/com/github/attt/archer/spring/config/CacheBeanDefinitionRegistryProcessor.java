package com.github.attt.archer.spring.config;


import com.github.attt.archer.CacheManager;
import com.github.attt.archer.cache.ShardingCache;
import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.components.api.Serializer;
import com.github.attt.archer.components.internal.InternalObjectValueSerializer;
import com.github.attt.archer.constants.Serialization;
import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.interceptor.InvocationInterceptor;
import com.github.attt.archer.loader.SingleLoader;
import com.github.attt.archer.annotation.metadata.EvictionMetadata;
import com.github.attt.archer.annotation.metadata.ListCacheMetadata;
import com.github.attt.archer.annotation.metadata.ObjectCacheMetadata;
import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;
import com.github.attt.archer.annotation.config.EvictionConfig;
import com.github.attt.archer.annotation.config.ListCacheConfig;
import com.github.attt.archer.annotation.config.ObjectCacheConfig;
import com.github.attt.archer.annotation.config.AbstractCacheConfig;
import com.github.attt.archer.stats.api.CacheEvent;
import com.github.attt.archer.stats.api.listener.CacheStatsListener;
import com.github.attt.archer.stats.collector.NamedCacheEventCollector;
import com.github.attt.archer.util.CacheUtils;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.*;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache bean definition registry processor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheBeanDefinitionRegistryProcessor implements BeanDefinitionRegistryPostProcessor, BeanClassLoaderAware {

    private static final Logger logger = LoggerFactory.getLogger(CacheBeanDefinitionRegistryProcessor.class);

    private final BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();

    /**
     * Use this map to avoid to create duplicated internal serializer
     */
    @SuppressWarnings("rawtypes")
    private final Map<String, InternalObjectValueSerializer> internalValueSerializers = new ConcurrentHashMap<>();

    /**
     * Use this map to save method signature mapping to operation source bean name
     * It will be passed to {@link CacheManager} bean after all operation source
     * registered.
     */
    private final Map<String, List<String>> methodSignatureToOperationName = new ConcurrentHashMap<>();

    private ClassLoader classLoader;

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
        try {
            registerCache(registry);
        } catch (Exception e) {
            throw new CacheBeanParsingException(e.getMessage(), e);
        }
    }

    /**
     * Iterate bean definitions and register
     * <p>
     * cache operation source
     * cache management
     * cache handler
     *
     * @param registry
     * @throws ClassNotFoundException
     */
    private void registerCache(final BeanDefinitionRegistry registry) throws ClassNotFoundException {

        String[] beanNames = registry.getBeanDefinitionNames();
        for (final String beanName : beanNames) {
            AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) registry.getBeanDefinition(beanName);
            Class<?> clazz;
            if (beanDefinition.hasBeanClass()) {
                clazz = beanDefinition.getBeanClass();
            } else {
                clazz = beanDefinition.resolveBeanClass(classLoader);
            }

            if (clazz != null) {
                Method[] methods = clazz.getMethods();
                for (final Method method : methods) {
                    List<Annotation> cacheAnnotations = ReflectionUtil.getCacheAnnotations(method);
                    for (Annotation annotation : cacheAnnotations) {
                        switch (ReflectionUtil.typeOf(annotation)) {
                            case OBJECT:
                                registerObjectCacheOperationSource(method, annotation, registry);
                                break;
                            case LIST:
                                registerListCacheOperationSource(method, annotation, registry);
                                break;
                            case EVICT:
                                List<Annotation> annotations = ReflectionUtil.getRepeatableCacheAnnotations(method);
                                if (annotations.size() > 0) {
                                    registerEvictionCacheOperationSource(method, annotations, registry);
                                }
                                break;
                            case NULL:
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    private void registerObjectCacheOperationSource(
            final Method method,
            final Annotation annotation,
            final BeanDefinitionRegistry registry
    ) {
        if (annotation == null) {
            return;
        }
        final String methodSignature = ReflectionUtil.getSignature(method, true, true);

        final Type returnType = method.getGenericReturnType();

        List<AbstractCacheMetadata> metadataList = CacheUtils.resolveMetadata(method, annotation);
        for (AbstractCacheMetadata abstractMetadata : metadataList) {
            final ObjectCacheMetadata metadata = (ObjectCacheMetadata) abstractMetadata;

            Type cacheEntityType = metadata.isMultiple() ? CacheUtils.parseCacheEntityType(method) : returnType;

            if (CacheManager.Config.valueSerialization == Serialization.HESSIAN || CacheManager.Config.valueSerialization == Serialization.JAVA) {
                if (!Serializable.class.isAssignableFrom(ReflectionUtil.toClass(cacheEntityType))) {
                    throw new CacheBeanParsingException("To use Hessian or Java serialization, " + cacheEntityType.getTypeName() + " must implement java.io.Serializable");
                }
            }

            logger.debug("CacheClass is : {}", cacheEntityType.getTypeName());

            // value serializer
            final String userValueSerializer = metadata.getValueSerializer();
            Object valueSerializer;
            if (CommonUtils.isEmpty(userValueSerializer)) {
                valueSerializer = internalValueSerializers.getOrDefault(cacheEntityType.getTypeName(),
                        new InternalObjectValueSerializer<>(cacheEntityType));
            } else {
                valueSerializer = new RuntimeBeanReference(userValueSerializer);
            }

            // register cache operation source bean definition
            AbstractBeanDefinition cacheOperationDefinition = BeanDefinitionBuilder.genericBeanDefinition(ObjectCacheConfig.class)
                    .addPropertyValue("metadata", metadata)
                    .addPropertyValue("loader", metadata.isMultiple() ? null : CacheUtils.resolveSingleLoader(method))
                    .addPropertyValue("multipleLoader", metadata.isMultiple() ? CacheUtils.resolveMultiLoader(method) : null)
                    .addPropertyValue("valueSerializer", valueSerializer)
                    .addPropertyValue("cacheEventCollector", new NamedCacheEventCollector(metadata.getMethodSignature()))
                    .getBeanDefinition();

            String operationName = beanNameGenerator.generateBeanName(cacheOperationDefinition, registry);
            registry.registerBeanDefinition(operationName, cacheOperationDefinition);

            // mapping method to operation source bean
            methodSignatureToOperationName.computeIfAbsent(methodSignature, sigKey -> new ArrayList<>()).add(operationName);
        }

    }


    private void registerListCacheOperationSource(
            final Method method,
            final Annotation annotation,
            final BeanDefinitionRegistry registry) {
        if (annotation == null) {
            return;
        }

        if (!ReflectionUtil.isCollectionOrArray(method.getReturnType())) {
            throw new CacheBeanParsingException("Listable cacheable method return type should be an array or Collection!");
        }

        final String methodSignature = ReflectionUtil.getSignature(method, true, true);

        final Type cacheEntityType = CacheUtils.parseCacheEntityType(method);

        if (CacheManager.Config.valueSerialization == Serialization.HESSIAN || CacheManager.Config.valueSerialization == Serialization.JAVA) {
            if (!Serializable.class.isAssignableFrom(ReflectionUtil.toClass(cacheEntityType))) {
                throw new CacheBeanParsingException("To use Hessian or Java serialization, " + cacheEntityType.getTypeName() + " must implement java.io.Serializable");
            }
        }

        List<AbstractCacheMetadata> metadataList = CacheUtils.resolveMetadata(method, annotation);
        for (AbstractCacheMetadata abstractMetadata : metadataList) {
            final ListCacheMetadata metadata = (ListCacheMetadata) abstractMetadata;

            logger.debug("CacheEntityClass is : {}", cacheEntityType.getTypeName());

            // create loader proxy
            SingleLoader<?> loader = CacheUtils.createListableCacheLoader();

            // operation source class
            BeanDefinitionBuilder cacheOperationDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ListCacheConfig.class);

            // value serializer
            final String userElementValueSerializer = metadata.getElementValueSerializer();
            Object valueSerializer;
            if (CommonUtils.isEmpty(userElementValueSerializer)) {
                valueSerializer = internalValueSerializers.getOrDefault(cacheEntityType.getTypeName(),
                        new InternalObjectValueSerializer<>(cacheEntityType));
            } else {
                valueSerializer = new RuntimeBeanReference(userElementValueSerializer);
            }

            // register cache operation source bean definition
            AbstractBeanDefinition cacheOperationDefinition = cacheOperationDefinitionBuilder
                    .addPropertyValue("metadata", metadata)
                    .addPropertyValue("loader", loader)
                    .addPropertyValue("valueSerializer", valueSerializer)
                    .addPropertyValue("cacheEventCollector", new NamedCacheEventCollector(metadata.getMethodSignature()))
                    .getBeanDefinition();

            String operationName = beanNameGenerator.generateBeanName(cacheOperationDefinition, registry);
            registry.registerBeanDefinition(operationName, cacheOperationDefinition);

            // mapping method to operation source bean
            methodSignatureToOperationName.computeIfAbsent(methodSignature, sigKey -> new ArrayList<>()).add(operationName);
        }
    }

    private void registerEvictionCacheOperationSource(
            final Method method,
            final List<Annotation> annotationList,
            final BeanDefinitionRegistry registry) {
        final String methodSignature = ReflectionUtil.getSignature(method, true, true);
        for (Annotation annotation : annotationList) {
            List<AbstractCacheMetadata> metadataList = CacheUtils.resolveMetadata(method, annotation);
            for (AbstractCacheMetadata abstractMetadata : metadataList) {
                EvictionMetadata metadata = (EvictionMetadata) abstractMetadata;

                // register cache OperationSource bean definition
                AbstractBeanDefinition evictionOperationDefinition = BeanDefinitionBuilder.genericBeanDefinition(EvictionConfig.class)
                        .addPropertyValue("metadata", metadata)
                        .addPropertyValue("cacheEventCollector", new NamedCacheEventCollector(metadata.getMethodSignature()))
                        .getBeanDefinition();

                String operationName = beanNameGenerator.generateBeanName(evictionOperationDefinition, registry);
                registry.registerBeanDefinition(operationName, evictionOperationDefinition);

                // mapping method to OperationSource bean
                methodSignatureToOperationName.computeIfAbsent(methodSignature, sigKey -> new ArrayList<>()).add(operationName);
            }
        }
    }


    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.addBeanPostProcessor(new CacheBeanPostProcessor());

        // register cache manager
        CacheManager cacheManager = new CacheManager();
        // pass reference to cacheManager
        cacheManager.setMethodSignatureToOperationSourceName(methodSignatureToOperationName);

        Map<String, KeyGenerator> keyGeneratorMap = beanFactory.getBeansOfType(KeyGenerator.class);
        cacheManager.setKeyGeneratorMap(keyGeneratorMap);

        Map<String, Serializer> serializerMap = beanFactory.getBeansOfType(Serializer.class);
        cacheManager.setSerializerMap(serializerMap);

        // sharding cache, it's a sharding delegate for all caches
        ShardingCache shardingCache = beanFactory.getBean(ShardingCache.SHARDING_CACHE_BEAN_NAME, ShardingCache.class);
        cacheManager.setShardingCache(shardingCache);

        // cache operations
        Map<String, AbstractCacheConfig> cacheOperationMap = beanFactory.getBeansOfType(AbstractCacheConfig.class);
        Map<String, EvictionConfig> evictionOperationMap = beanFactory.getBeansOfType(EvictionConfig.class);

        // register cache stats listeners
        if (CacheManager.Config.metricsEnabled) {

            Map<String, CacheStatsListener> statsListenerMap = beanFactory.getBeansOfType(CacheStatsListener.class);
            cacheManager.setStatsListenerMap(statsListenerMap);

            for (AbstractCacheConfig cacheOperation : cacheOperationMap.values()) {
                for (CacheStatsListener<CacheEvent> statsListener : statsListenerMap.values()) {
                    cacheOperation.getCacheEventCollector().register(statsListener);
                }
            }

            for (EvictionConfig evictionOperation : evictionOperationMap.values()) {
                for (CacheStatsListener<CacheEvent> statsListener : statsListenerMap.values()) {
                    evictionOperation.getCacheEventCollector().register(statsListener);
                }
            }
        }

        cacheManager.setCacheOperationMap(cacheOperationMap);
        cacheManager.setEvictionOperationMap(evictionOperationMap);

        beanFactory.registerSingleton(CacheManager.INTERNAL_CACHE_MANAGER_BEAN_NAME, cacheManager);

        cacheManager.initialized();
        logger.debug(
                CacheManager.INTERNAL_CACHE_MANAGER_BEAN_NAME + " ========>>> " +
                        cacheManager.initializedInfo());

        InvocationInterceptor.init(cacheManager);
    }
}
