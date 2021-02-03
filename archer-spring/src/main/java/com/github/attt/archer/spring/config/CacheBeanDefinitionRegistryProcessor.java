package com.github.attt.archer.spring.config;


import com.github.attt.archer.CacheManager;
import com.github.attt.archer.cache.ShardingCache;
import com.github.attt.archer.cache.KeyGenerator;
import com.github.attt.archer.cache.Serializer;
import com.github.attt.archer.cache.internal.InternalObjectValueSerializer;
import com.github.attt.archer.constants.Serialization;
import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.interceptor.InvocationInterceptor;
import com.github.attt.archer.loader.SingleLoader;
import com.github.attt.archer.annotation.metadata.EvictionMetadata;
import com.github.attt.archer.annotation.metadata.ListCacheMetadata;
import com.github.attt.archer.annotation.metadata.ObjectCacheMetadata;
import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;
import com.github.attt.archer.annotation.config.EvictionProperties;
import com.github.attt.archer.annotation.config.ListCacheProperties;
import com.github.attt.archer.annotation.config.ObjectCacheProperties;
import com.github.attt.archer.annotation.config.AbstractCacheProperties;
import com.github.attt.archer.metrics.api.CacheEvent;
import com.github.attt.archer.metrics.api.listener.CacheMetricsListener;
import com.github.attt.archer.metrics.collector.NamedCacheEventCollector;
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
     * 复用无状态的valueSerializer，每个类型（key）只创建一个实例
     */
    @SuppressWarnings("rawtypes")
    private final Map<String, InternalObjectValueSerializer> internalValueSerializers = new ConcurrentHashMap<>();

    /**
     * 保存长方法签名到properties的bean name的映射。<br>
     * 长方法签名是指[类全名+方法全名+方法参数类型全名]，如：<br>
     * com.sample.module.ClassA.methodB(java.lang.Long, com.sample.ModelC, java.util.List) <br>
     *
     * @see ReflectionUtil#getSignature(Method, boolean, boolean)
     */
    private final Map<String, List<String>> methodSignatureToPropertiesName = new ConcurrentHashMap<>();

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
     * 扫描spring管理的所有bean，找出所有启用缓存注解的类和方法，<br>
     * 解析出对应的metadata，根据metadata的配置信息从spring中找到 <br>
     * 符合条件的组件装配成properties，再将properties注册到spring中。
     *
     * @param registry spring的bean定义注册器
     * @throws ClassNotFoundException
     * @see ReflectionUtil
     * @see AbstractCacheMetadata
     * @see AbstractCacheProperties
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
                                registerObjectCacheProperties(method, annotation, registry);
                                break;
                            case LIST:
                                registerListCacheProperties(method, annotation, registry);
                                break;
                            case EVICT:
                                List<Annotation> annotations = ReflectionUtil.getRepeatableCacheAnnotations(method);
                                if (annotations.size() > 0) {
                                    registerEvictionProperties(method, annotations, registry);
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

    private void registerObjectCacheProperties(
            final Method method,
            final Annotation annotation,
            final BeanDefinitionRegistry registry
    ) {
        if (annotation == null) {
            return;
        }
        // 获取方法的长签名
        final String methodSignature = ReflectionUtil.getSignature(method, true, true);

        // 获取返回类型信息
        final Type returnType = method.getGenericReturnType();

        // 通过方法和其注解解析出metadata
        List<AbstractCacheMetadata> metadataList = CacheUtils.resolveMetadata(method, annotation);
        for (AbstractCacheMetadata abstractMetadata : metadataList) {
            final ObjectCacheMetadata metadata = (ObjectCacheMetadata) abstractMetadata;

            // 如果是multi缓存，返回值可能为集合或数组，需要解析出元素类型作为缓存类型
            Type cacheEntityType = metadata.isMultiple() ? CacheUtils.parseCacheEntityType(method) : returnType;

            // 检查序列化方式是否支持当前的缓存类型
            if (CacheManager.Config.valueSerialization == Serialization.HESSIAN || CacheManager.Config.valueSerialization == Serialization.JAVA) {
                if (!Serializable.class.isAssignableFrom(ReflectionUtil.toClass(cacheEntityType))) {
                    throw new CacheBeanParsingException("To use Hessian or Java serialization, " + cacheEntityType.getTypeName() + " must implement java.io.Serializable");
                }
            }

            logger.debug("CacheClass is : {}", cacheEntityType.getTypeName());

            // 用户自定义的value serializer的bean name
            final String userValueSerializer = metadata.getValueSerializer();
            Object valueSerializer = chooseValueSerializer(userValueSerializer, cacheEntityType);

            // 注册cache properties bean定义
            AbstractBeanDefinition cachePropertiesDefinition = BeanDefinitionBuilder.genericBeanDefinition(ObjectCacheProperties.class)
                    .addPropertyValue("metadata", metadata)
                    .addPropertyValue("loader", metadata.isMultiple() ? null : CacheUtils.resolveSingleLoader(method))
                    .addPropertyValue("multipleLoader", metadata.isMultiple() ? CacheUtils.resolveMultiLoader(method) : null)
                    .addPropertyValue("valueSerializer", valueSerializer)
                    .addPropertyValue("cacheEventCollector", new NamedCacheEventCollector(metadata.getMethodSignature()))
                    .getBeanDefinition();

            String propertiesName = beanNameGenerator.generateBeanName(cachePropertiesDefinition, registry);
            registry.registerBeanDefinition(propertiesName, cachePropertiesDefinition);

            // 将方法长签名与注册就绪的properties的bean name映射
            methodSignatureToPropertiesName.computeIfAbsent(methodSignature, sigKey -> new ArrayList<>()).add(propertiesName);
        }

    }


    private void registerListCacheProperties(
            final Method method,
            final Annotation annotation,
            final BeanDefinitionRegistry registry) {
        if (annotation == null) {
            return;
        }

        // 检查返回类型是否是数组或集合
        if (!ReflectionUtil.isCollectionOrArray(method.getReturnType())) {
            throw new CacheBeanParsingException("Listable cacheable method return type should be an array or java.util.Collection!");
        }

        // 获取方法的长签名
        final String methodSignature = ReflectionUtil.getSignature(method, true, true);

        // 获取返回类型信息
        final Type cacheEntityType = CacheUtils.parseCacheEntityType(method);

        // 检查序列化方式是否支持当前的缓存类型
        if (CacheManager.Config.valueSerialization == Serialization.HESSIAN || CacheManager.Config.valueSerialization == Serialization.JAVA) {
            if (!Serializable.class.isAssignableFrom(ReflectionUtil.toClass(cacheEntityType))) {
                throw new CacheBeanParsingException("To use Hessian or Java serialization, " + cacheEntityType.getTypeName() + " must implement java.io.Serializable");
            }
        }

        // 通过方法和其注解解析出metadata
        List<AbstractCacheMetadata> metadataList = CacheUtils.resolveMetadata(method, annotation);
        for (AbstractCacheMetadata abstractMetadata : metadataList) {
            final ListCacheMetadata metadata = (ListCacheMetadata) abstractMetadata;

            logger.debug("CacheEntityClass is : {}", cacheEntityType.getTypeName());

            // 创建加载器代理
            SingleLoader<?> loader = CacheUtils.createListableCacheLoader();

            // list cache properties class
            BeanDefinitionBuilder listCacheProperties = BeanDefinitionBuilder.genericBeanDefinition(ListCacheProperties.class);

            // 用户自定义的value serializer的bean name
            final String userElementValueSerializer = metadata.getElementValueSerializer();
            Object valueSerializer = chooseValueSerializer(userElementValueSerializer, cacheEntityType);

            // 注册list cache properties bean定义
            AbstractBeanDefinition cachePropertiesDefinition = listCacheProperties
                    .addPropertyValue("metadata", metadata)
                    .addPropertyValue("loader", loader)
                    .addPropertyValue("valueSerializer", valueSerializer)
                    .addPropertyValue("cacheEventCollector", new NamedCacheEventCollector(metadata.getMethodSignature()))
                    .getBeanDefinition();

            String propertiesName = beanNameGenerator.generateBeanName(cachePropertiesDefinition, registry);
            registry.registerBeanDefinition(propertiesName, cachePropertiesDefinition);

            // 将方法长签名与注册就绪的properties的bean name映射
            methodSignatureToPropertiesName.computeIfAbsent(methodSignature, sigKey -> new ArrayList<>()).add(propertiesName);
        }
    }

    private void registerEvictionProperties(
            final Method method,
            final List<Annotation> annotationList,
            final BeanDefinitionRegistry registry) {
        final String methodSignature = ReflectionUtil.getSignature(method, true, true);
        for (Annotation annotation : annotationList) {
            List<AbstractCacheMetadata> metadataList = CacheUtils.resolveMetadata(method, annotation);
            for (AbstractCacheMetadata abstractMetadata : metadataList) {
                EvictionMetadata metadata = (EvictionMetadata) abstractMetadata;

                // 注册eviction properties bean定义
                AbstractBeanDefinition evictionPropertiesDefinition = BeanDefinitionBuilder.genericBeanDefinition(EvictionProperties.class)
                        .addPropertyValue("metadata", metadata)
                        .addPropertyValue("cacheEventCollector", new NamedCacheEventCollector(metadata.getMethodSignature()))
                        .getBeanDefinition();

                String propertiesName = beanNameGenerator.generateBeanName(evictionPropertiesDefinition, registry);
                registry.registerBeanDefinition(propertiesName, evictionPropertiesDefinition);

                // 将方法长签名与注册就绪的properties的bean name映射
                methodSignatureToPropertiesName.computeIfAbsent(methodSignature, sigKey -> new ArrayList<>()).add(propertiesName);
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

        // 创建cache manager
        CacheManager cacheManager = new CacheManager();
        // pass reference to cacheManager
        cacheManager.setMethodSignatureToPropertiesName(methodSignatureToPropertiesName);

        Map<String, KeyGenerator> keyGeneratorMap = beanFactory.getBeansOfType(KeyGenerator.class);
        cacheManager.setKeyGeneratorMap(keyGeneratorMap);

        Map<String, Serializer> serializerMap = beanFactory.getBeansOfType(Serializer.class);
        cacheManager.setSerializerMap(serializerMap);

        // sharding cache, it's a sharding delegate for all caches
        ShardingCache shardingCache = beanFactory.getBean(ShardingCache.SHARDING_CACHE_BEAN_NAME, ShardingCache.class);
        cacheManager.setShardingCache(shardingCache);

        // cache operations
        Map<String, AbstractCacheProperties> cachePropertiesMap = beanFactory.getBeansOfType(AbstractCacheProperties.class);
        Map<String, EvictionProperties> evictionPropertiesMap = beanFactory.getBeansOfType(EvictionProperties.class);

        // register cache stats listeners
        if (CacheManager.Config.metricsEnabled) {

            Map<String, CacheMetricsListener> statsListenerMap = beanFactory.getBeansOfType(CacheMetricsListener.class);
            cacheManager.setStatsListenerMap(statsListenerMap);

            for (AbstractCacheProperties cacheProperties : cachePropertiesMap.values()) {
                for (CacheMetricsListener<CacheEvent> statsListener : statsListenerMap.values()) {
                    cacheProperties.getCacheEventCollector().register(statsListener);
                }
            }

            for (EvictionProperties evictionProperties : evictionPropertiesMap.values()) {
                for (CacheMetricsListener<CacheEvent> statsListener : statsListenerMap.values()) {
                    evictionProperties.getCacheEventCollector().register(statsListener);
                }
            }
        }

        cacheManager.setCachePropertiesMap(cachePropertiesMap);
        cacheManager.setEvictionPropertiesMap(evictionPropertiesMap);

        // 将cache manager交由spring托管
        beanFactory.registerSingleton(CacheManager.INTERNAL_CACHE_MANAGER_BEAN_NAME, cacheManager);

        cacheManager.initialized();
        logger.debug(
                CacheManager.INTERNAL_CACHE_MANAGER_BEAN_NAME + " ========>>> " +
                        cacheManager.initializedInfo());

        InvocationInterceptor.init(cacheManager);
    }

    private Object chooseValueSerializer(String userValueSerializer, Type cacheEntityType) {
        Object valueSerializer;
        if (CommonUtils.isEmpty(userValueSerializer)) {
            valueSerializer = internalValueSerializers.compute(cacheEntityType.getTypeName(), (key, internalObjectValueSerializer) -> {
                if (internalObjectValueSerializer == null) {
                    internalObjectValueSerializer = new InternalObjectValueSerializer<>(cacheEntityType);
                    internalValueSerializers.put(key, internalObjectValueSerializer);
                }
                return internalObjectValueSerializer;
            });
        } else {
            valueSerializer = new RuntimeBeanReference(userValueSerializer);
        }
        return valueSerializer;
    }
}
