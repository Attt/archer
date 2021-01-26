package com.github.attt.archer;

import com.github.attt.archer.cache.CacheInitializerDelegate;
import com.github.attt.archer.cache.annotation.Cache;
import com.github.attt.archer.cache.annotation.CacheList;
import com.github.attt.archer.cache.api.CacheInitializer;
import com.github.attt.archer.cache.api.CacheShard;
import com.github.attt.archer.cache.internal.ShardingCache;
import com.github.attt.archer.cache.internal.ShardingCacheConfigure;
import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.components.api.ValueSerializer;
import com.github.attt.archer.components.internal.InternalObjectValueSerializer;
import com.github.attt.archer.constants.Serialization;
import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.exception.CacheOperationException;
import com.github.attt.archer.invocation.InvocationInterceptor;
import com.github.attt.archer.metadata.EvictionMetadata;
import com.github.attt.archer.metadata.ListCacheMetadata;
import com.github.attt.archer.metadata.ObjectCacheMetadata;
import com.github.attt.archer.metadata.api.AbstractCacheMetadata;
import com.github.attt.archer.operation.EvictionOperation;
import com.github.attt.archer.operation.ListCacheOperation;
import com.github.attt.archer.operation.ObjectCacheOperation;
import com.github.attt.archer.stats.api.CacheEventCollector;
import com.github.attt.archer.stats.api.listener.CacheStatsListener;
import com.github.attt.archer.stats.collector.NamedCacheEventCollector;
import com.github.attt.archer.util.CacheUtils;
import com.github.attt.archer.util.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The starter of archer cache framework
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@SuppressWarnings("all")
public class Archer {

    public static final Logger logger = LoggerFactory.getLogger(Archer.class);

    /**
     * Use this map to avoid creating duplicated internal serializer
     */
    @SuppressWarnings("rawtypes")
    private final Map<String, InternalObjectValueSerializer> internalValueSerializers = new ConcurrentHashMap<>();


    private final Map<String, KeyGenerator> keyGeneratorMap = new ConcurrentHashMap<>();

    private final Map<String, ValueSerializer> valueSerializerMap = new ConcurrentHashMap<>();

    private final Set<CacheStatsListener> statsListeners = new ConcurrentSkipListSet<>();

    private final CacheManager cacheManager = new CacheManager();

    private CacheInitializer cacheInitializer;

    private final List<CacheShard> cacheShards = Collections.synchronizedList(new ArrayList<>());

    private Archer() {
    }

    public static Archer create(String... basePackages) {
        ReflectionUtil.forPackage(basePackages);
        return new Archer();
    }

    public Archer addValueSerializer(String name, ValueSerializer valueSerializer) {
        valueSerializerMap.put(name, valueSerializer);
        return this;
    }

    public Archer addKeyGenerator(String name, KeyGenerator keyGenerator) {
        keyGeneratorMap.put(name, keyGenerator);
        return this;
    }

    public Archer addStatsListener(CacheStatsListener statsListener) {
        this.statsListeners.add(statsListener);
        return this;
    }

    public Archer setCacheConfigs(List<? extends CacheShard> configs) {
        cacheShards.addAll(configs);
        return this;
    }

    public Archer setCacheInitializer(CacheInitializer initializer) {
        this.cacheInitializer = initializer;
        return this;
    }

    public Archer addCacheConfig(CacheShard shard) {
        cacheShards.add(shard);
        return this;
    }

    public Starter init() {
        cacheManager.getSerializerMap().putAll(valueSerializerMap);
        cacheManager.getKeyGeneratorMap().putAll(keyGeneratorMap);

        sharding();
        cacheManager.initialized();

        InvocationInterceptor.init(cacheManager);

        return new Starter(this);
    }

    public static void serialization(Serialization serialization) {
        CacheManager.Config.valueSerialization = serialization;
    }

    public static void enableMetrics() {
        CacheManager.Config.metricsEnabled = true;
    }

    public static void enableMetrics(boolean enable) {
        CacheManager.Config.metricsEnabled = enable;
    }

    private void eviction(Class<?> service, String signature, Method declaredMethod) {
        if (cacheManager.getMethodSignatureToOperationSourceName().containsKey(signature)) {
            return;
        }
        List<Annotation> cacheEvictAnnotations = ReflectionUtil.getRepeatableCacheAnnotations(declaredMethod);
        for (Annotation cacheEvictAnnotation : cacheEvictAnnotations) {
            List<AbstractCacheMetadata> metadataList = CacheUtils.resolveMetadata(declaredMethod, cacheEvictAnnotation);
            for (AbstractCacheMetadata metadata : metadataList) {
                EvictionOperation evictionOperation = new EvictionOperation();
                evictionOperation.setMetadata((EvictionMetadata) metadata);
                CacheEventCollector cacheEventCollector = new NamedCacheEventCollector(metadata.getMethodSignature());
                if (CacheManager.Config.metricsEnabled) {
                    for (CacheStatsListener statsListener : this.statsListeners) {
                        cacheEventCollector.register(statsListener);
                    }
                }
                evictionOperation.setCacheEventCollector(cacheEventCollector);
                evictionOperation.initialized();
                String name = "eviction" + UUID.randomUUID().toString();
                cacheManager.getEvictionOperationMap().put(name, evictionOperation);
                cacheManager.getMethodSignatureToOperationSourceName().computeIfAbsent(signature, s -> new ArrayList<>()).add(name);
            }
        }
    }


    private void cache(Class<?> service, String signature, Method declaredMethod) {
        if (cacheManager.getMethodSignatureToOperationSourceName().containsKey(signature)) {
            return;
        }
        List<Annotation> annotations = ReflectionUtil.getCacheAnnotations(declaredMethod, Cache.class);
        for (Annotation annotation : annotations) {
            if (annotation != null) {
                List<AbstractCacheMetadata> metadataList = CacheUtils.resolveMetadata(declaredMethod, annotation);
                for (AbstractCacheMetadata abstractMetadata : metadataList) {
                    ObjectCacheMetadata metadata = (ObjectCacheMetadata) abstractMetadata;
                    ObjectCacheOperation cacheOperation = new ObjectCacheOperation();
                    cacheOperation.setMetadata(metadata);

                    if (metadata.isMultiple()) {
                        cacheOperation.setMultipleLoader(CacheUtils.resolveMultiLoader(declaredMethod));
                    } else {
                        cacheOperation.setLoader(CacheUtils.resolveSingleLoader(declaredMethod));
                    }

                    Type cacheEntityType = metadata.isMultiple() ? CacheUtils.parseCacheEntityType(declaredMethod) : declaredMethod.getGenericReturnType();
                    if (CacheManager.Config.valueSerialization == Serialization.HESSIAN || CacheManager.Config.valueSerialization == Serialization.JAVA) {
                        if (!Serializable.class.isAssignableFrom(ReflectionUtil.toClass(cacheEntityType))) {
                            throw new CacheBeanParsingException("To use Hessian or Java serialization, " + cacheEntityType.getTypeName() + " must implement java.io.Serializable");
                        }
                    }

                    ValueSerializer userValueSerializer = StringUtils.isEmpty(metadata.getValueSerializer()) ? null : valueSerializerMap.getOrDefault(metadata.getValueSerializer(), null);
                    if (userValueSerializer != null) {
                        cacheOperation.setValueSerializer(userValueSerializer);
                    } else {
                        internalValueSerializers.computeIfAbsent(cacheEntityType.getTypeName(), new Function<String, InternalObjectValueSerializer>() {
                            @Override
                            public InternalObjectValueSerializer apply(String s) {
                                return new InternalObjectValueSerializer(cacheEntityType);
                            }
                        });
                        cacheOperation.setValueSerializer(
                                internalValueSerializers.get(cacheEntityType.getTypeName())
                        );
                    }

                    CacheEventCollector cacheEventCollector = new NamedCacheEventCollector(metadata.getMethodSignature());
                    if (CacheManager.Config.metricsEnabled) {
                        for (CacheStatsListener statsListener : this.statsListeners) {
                            cacheEventCollector.register(statsListener);
                        }
                    }
                    cacheOperation.setCacheEventCollector(cacheEventCollector);

                    cacheOperation.initialized();
                    String name = "cache" + UUID.randomUUID().toString();
                    cacheManager.getCacheOperationMap().put(name, cacheOperation);
                    cacheManager.getMethodSignatureToOperationSourceName().computeIfAbsent(signature, s -> new ArrayList<>()).add(name);
                }
            }
        }
    }

    private void listCache(Class<?> service, String signature, Method declaredMethod) {
        if (cacheManager.getMethodSignatureToOperationSourceName().containsKey(signature)) {
            return;
        }
        List<Annotation> annotations = ReflectionUtil.getCacheAnnotations(declaredMethod, CacheList.class);
        for (Annotation annotation : annotations) {
            if (annotation != null) {
                List<AbstractCacheMetadata> metadataList = CacheUtils.resolveMetadata(declaredMethod, annotation);
                for (AbstractCacheMetadata abstractMetadata : metadataList) {
                    ListCacheMetadata metadata = (ListCacheMetadata) abstractMetadata;
                    ListCacheOperation listCacheOperation = new ListCacheOperation();
                    listCacheOperation.setMetadata(metadata);
                    listCacheOperation.setLoader(CacheUtils.createListableCacheLoader());
                    Type cacheEntityType = CacheUtils.parseCacheEntityType(declaredMethod);
                    if (CacheManager.Config.valueSerialization == Serialization.HESSIAN || CacheManager.Config.valueSerialization == Serialization.JAVA) {
                        if (!Serializable.class.isAssignableFrom(ReflectionUtil.toClass(cacheEntityType))) {
                            throw new CacheBeanParsingException("To use Hessian or Java serialization, " + cacheEntityType.getTypeName() + " must implement java.io.Serializable");
                        }
                    }

                    ValueSerializer userValueSerializer = StringUtils.isEmpty(metadata.getElementValueSerializer()) ? null : valueSerializerMap.getOrDefault(metadata.getElementValueSerializer(), null);
                    if (userValueSerializer != null) {
                        listCacheOperation.setValueSerializer(userValueSerializer);
                    } else {
                        internalValueSerializers.computeIfAbsent(cacheEntityType.getTypeName(), new Function<String, InternalObjectValueSerializer>() {
                            @Override
                            public InternalObjectValueSerializer apply(String s) {
                                return new InternalObjectValueSerializer(cacheEntityType);
                            }
                        });
                        listCacheOperation.setValueSerializer(
                                internalValueSerializers.get(cacheEntityType.getTypeName())
                        );
                    }

                    CacheEventCollector cacheEventCollector = new NamedCacheEventCollector(metadata.getMethodSignature());
                    if (CacheManager.Config.metricsEnabled) {
                        for (CacheStatsListener statsListener : this.statsListeners) {
                            cacheEventCollector.register(statsListener);
                        }
                    }
                    listCacheOperation.setCacheEventCollector(cacheEventCollector);

                    listCacheOperation.initialized();
                    String name = "listCache" + UUID.randomUUID().toString();
                    cacheManager.getCacheOperationMap().put(name, listCacheOperation);
                    cacheManager.getMethodSignatureToOperationSourceName().computeIfAbsent(signature, s -> new ArrayList<>()).add(name);
                }
            }
        }
    }

    private void sharding() {
        if (cacheInitializer == null) {
            cacheInitializer = new CacheInitializerDelegate();
        }
        ShardingCacheConfigure shardingCacheConfigure = new ShardingCacheConfigure(cacheInitializer, cacheShards);
        shardingCacheConfigure.init();
        ShardingCache shardingCache = new ShardingCache(shardingCacheConfigure);
        cacheManager.setShardingCache(shardingCache);
    }

    private Supplier<Object> methodInvoker(Object instance, Method method, Object[] args) {
        return () -> {
            try {
                method.setAccessible(true);
                return method.invoke(instance, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("Can't invoke method " + method.getName(), e);
                return null;
            }
        };
    }


    public static class Starter {

        private Archer archer;

        private Starter(Archer archer) {
            this.archer = archer;
        }

        public <T> T start(Class<T> service) {
            T instance;
            try {
                instance = service.newInstance();
                return start(service, instance);
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Can't create service instance.", e);
                return null;
            }
        }

        public <T> T start(Class<T> serviceType, T instance) {

            Method[] declaredMethods = instance.getClass().getMethods();
            for (Method declaredMethod : declaredMethods) {
                String signature = ReflectionUtil.getSignatureForCache(declaredMethod);
                archer.eviction(instance.getClass(), signature, declaredMethod);
                archer.cache(instance.getClass(), signature, declaredMethod);
                archer.listCache(instance.getClass(), signature, declaredMethod);
            }
            if (serviceType.isInterface()) {
                // JDK proxy
                InvocationHandler invocationHandler = (proxy, method, args) -> InvocationInterceptor.INSTANCE.invoke(proxy, instance, archer.methodInvoker(instance, method, args), method, args);
                return (T) Proxy.newProxyInstance(serviceType.getClassLoader(), new Class[]{serviceType}, invocationHandler);
            } else {
                // cglib proxy
                try {
                    Class<?> enhancerClass = Class.forName("net.sf.cglib.proxy.Enhancer");
                    Object enhancer = enhancerClass.newInstance();
                    Method setSuperclass = enhancerClass.getDeclaredMethod("setSuperclass", Class.class);
                    setSuperclass.invoke(enhancer, serviceType);

                    Class<?> callBackClass = Class.forName("net.sf.cglib.proxy.Callback");
                    Method setCallback = enhancerClass.getDeclaredMethod("setCallback", callBackClass);

                    Class<?> methodCallBackClass = Class.forName("net.sf.cglib.proxy.MethodInterceptor");

                    InvocationHandler anonymousMethodCallback = (p, m, a) -> {
                        Object target = a[0];
                        Method method = (Method) a[1];
                        Object[] args = (Object[]) a[2];
                        return InvocationInterceptor.INSTANCE.invoke(target, instance, archer.methodInvoker(instance, method, args), method, args);
                    };
                    setCallback.invoke(enhancer, Proxy.newProxyInstance(methodCallBackClass.getClassLoader(), new Class[]{methodCallBackClass}, anonymousMethodCallback));

                    Method create = enhancerClass.getDeclaredMethod("create");
                    return (T) create.invoke(enhancer);
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
                    logger.error("To user class not implement interface, you should involve cglib dependency.");
                    throw new CacheOperationException(e);
                }
            }
        }
    }

}
