package com.github.attt.archer.util;

import com.github.attt.archer.annotation.*;
import com.github.attt.archer.annotation.extra.MapTo;
import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.exception.CacheOperationException;
import com.github.attt.archer.loader.MultipleLoader;
import com.github.attt.archer.loader.SingleLoader;
import com.github.attt.archer.metadata.EvictionMetadata;
import com.github.attt.archer.metadata.ListCacheMetadata;
import com.github.attt.archer.metadata.ObjectCacheMetadata;
import com.github.attt.archer.metadata.api.AbstractCacheMetadata;
import com.github.attt.archer.processor.context.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Cache resolver
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheResolver {

    private static final Logger logger = LoggerFactory.getLogger(CacheResolver.class);

    public static AbstractCacheMetadata resolveMetadata(Method method, Annotation annotation) {
        Class<?> methodClass = method.getDeclaringClass();
        Cacheable cacheable = ReflectionUtil.getCacheAnnotation(methodClass, Cacheable.class);
        return resolveMetadata(cacheable, method, annotation);
    }

    public static AbstractCacheMetadata resolveMetadata(Cacheable serviceCacheable, Method method, Annotation annotation) {
        String keyPrefix = "";
        String valueSerializer = "";
        String keyGenerator = "";
        String area = "";
        if (serviceCacheable != null) {
            keyPrefix = CommonUtils.isNotEmpty(serviceCacheable.prefix()) ? serviceCacheable.prefix() : serviceCacheable.value();
            valueSerializer = serviceCacheable.valueSerializer();
            keyGenerator = serviceCacheable.keyGenerator();
            area = serviceCacheable.area();
        }
        if (annotation instanceof Cache || annotation instanceof CacheMulti) {
            return resolveObjectCacheMetadata(method, annotation, keyPrefix, keyGenerator, valueSerializer, annotation, area);
        } else if (annotation instanceof CacheList) {
            CacheList cacheList = (CacheList) annotation;
            return resolveListableCacheMetadata(method, annotation, keyPrefix, keyGenerator, valueSerializer, cacheList, area);
        } else if (annotation instanceof Evict) {
            Evict evict = (Evict) annotation;
            return resolveCacheEvictMetadata(method, annotation, keyPrefix, keyGenerator, evict, area);
        } else if (annotation instanceof EvictMulti) {
            EvictMulti evict = (EvictMulti) annotation;
            return resolveCacheEvictMultiMetadata(method, annotation, keyPrefix, keyGenerator, evict, area);
        } else {
            throw new CacheBeanParsingException("Unsupported cache annotation : " + annotation);
        }
    }


    private
    static ObjectCacheMetadata
    resolveObjectCacheMetadata(Method method, Annotation cacheAnnotation, String keyPrefix, String keyGenerator, String valueSerializer, Annotation annotation, String globalArea) {
        ObjectCacheMetadata metadata = new ObjectCacheMetadata();

        String key, condition, aKeyGenerator, aValueSerializer, area;
        boolean overwrite, breakdownProtect;
        long expiration, breakdownProtectTimeout;
        TimeUnit expirationTimeUnit, breakdownProtectTimeoutTimeUnit;
        if (annotation instanceof Cache) {
            Cache cache = (Cache) annotation;
            key = cache.key();
            condition = cache.condition();
            aKeyGenerator = cache.keyGenerator();
            aValueSerializer = cache.valueSerializer();
            overwrite = cache.overwrite();
            breakdownProtect = cache.breakdownProtect();
            expiration = cache.expiration();
            breakdownProtectTimeout = cache.breakdownProtectTimeout();
            expirationTimeUnit = cache.expirationTimeUnit();
            breakdownProtectTimeoutTimeUnit = cache.breakdownProtectTimeUnit();
            area = cache.area();
        } else {
            CacheMulti cacheMulti = (CacheMulti) annotation;
            key = cacheMulti.elementKey();
            condition = cacheMulti.condition();
            aKeyGenerator = cacheMulti.keyGenerator();
            aValueSerializer = cacheMulti.valueSerializer();
            overwrite = cacheMulti.overwrite();
            breakdownProtect = cacheMulti.breakdownProtect();
            expiration = cacheMulti.expiration();
            breakdownProtectTimeout = cacheMulti.breakdownProtectTimeout();
            expirationTimeUnit = cacheMulti.expirationTimeUnit();
            breakdownProtectTimeoutTimeUnit = cacheMulti.breakdownProtectTimeUnit();
            area = cacheMulti.elementArea();

            metadata.setOrderBy(cacheMulti.orderBy());
            metadata.setMultiple(true);
        }
        resolveCommonMetadata(
                metadata,
                method,
                cacheAnnotation,
                keyPrefix,
                key,
                condition,
                resolveValue(keyGenerator, aKeyGenerator),
                resolveValue(globalArea, area)
        );

        metadata.setInvokeAnyway(overwrite);
        metadata.setExpirationInMillis(expirationTimeUnit.toMillis(expiration));
        metadata.setBreakdownProtect(breakdownProtect);
        metadata.setBreakdownProtectTimeoutInMillis(breakdownProtectTimeoutTimeUnit.toMillis(breakdownProtectTimeout));
        metadata.setValueSerializer(resolveValue(valueSerializer, aValueSerializer));
        return metadata;
    }

    private
    static ListCacheMetadata
    resolveListableCacheMetadata(Method method, Annotation cacheAnnotation, String keyPrefix, String keyGenerator, String valueSerializer, CacheList cacheList, String globalArea) {
        ListCacheMetadata metadata = new ListCacheMetadata();
        resolveCommonMetadata(
                metadata,
                method,
                cacheAnnotation,
                keyPrefix,
                cacheList.key(),
                cacheList.condition(),
                resolveValue(keyGenerator, cacheList.keyGenerator()),
                resolveValue(globalArea, cacheList.area())
        );
        metadata.setElementKey(cacheList.elementKey());
        metadata.setElementKeyGenerator(cacheList.elementKeyGenerator());
        metadata.setInvokeAnyway(cacheList.overwrite());
        metadata.setElementValueSerializer(resolveValue(valueSerializer, cacheList.elementValueSerializer()));
        metadata.setExpirationInMillis(cacheList.expirationTimeUnit().toMillis(cacheList.expiration()));
        metadata.setBreakdownProtect(cacheList.breakdownProtect());
        metadata.setBreakdownProtectTimeoutInMillis(cacheList.breakdownProtectTimeUnit().toMillis(cacheList.breakdownProtectTimeout()));
        metadata.setElementArea(cacheList.elementArea());
        return metadata;
    }

    private
    static EvictionMetadata
    resolveCacheEvictMetadata(Method method, Annotation cacheAnnotation, String keyPrefix, String keyGenerator, Evict evict, String globalArea) {
        EvictionMetadata metadata = new EvictionMetadata();
        resolveCommonMetadata(
                metadata,
                method,
                cacheAnnotation,
                keyPrefix,
                evict.key(),
                evict.condition(),
                resolveValue(keyGenerator, evict.keyGenerator()),
                resolveValue(globalArea, evict.area())
        );
        metadata.setAll(evict.all());
        metadata.setAfterInvocation(evict.afterInvocation());
        return metadata;
    }

    private
    static EvictionMetadata
    resolveCacheEvictMultiMetadata(Method method, Annotation cacheAnnotation, String keyPrefix, String keyGenerator, EvictMulti evict, String globalArea) {
        EvictionMetadata metadata = new EvictionMetadata();
        resolveCommonMetadata(
                metadata,
                method,
                cacheAnnotation,
                keyPrefix,
                evict.elementKey(),
                evict.condition(),
                resolveValue(keyGenerator, evict.keyGenerator()),
                resolveValue(globalArea, evict.elementArea())
        );
        metadata.setMultiple(true);
        metadata.setAfterInvocation(evict.afterInvocation());
        return metadata;
    }

    private
    static void
    resolveCommonMetadata(AbstractCacheMetadata metadata, Method method, Annotation cacheAnnotation, String keyPrefix, String key, String condition, String keyGenerator, String area) {
        metadata.setCacheAnnotation(cacheAnnotation);
        metadata.setKeyPrefix(keyPrefix);
        metadata.setKey(key);
        metadata.setCondition(condition);
        metadata.setKeyGenerator(keyGenerator);
        metadata.setMethodSignature(ReflectionUtil.getSignature(method));
        metadata.setArea(area);
    }

    private static String resolveValue(String global, String instant) {
        return CommonUtils.isEmpty(instant) ? global : instant;
    }

    public static Type parseCacheEntityType(Method method) {
        Type type = ReflectionUtil.getComponentOrArgumentType(method);
        if (type == null) {
            throw new RuntimeException("Listable/Multiple cache class (return type) should be collection or array");
        }
        return type;
    }

    public static SingleLoader<?> createListableCacheLoader() {
        InvocationHandler invocationHandler = (proxy, method1, args) -> {
            Object o = ((InvocationContext) args[0]).getMethodInvoker().get();
            if (o == null) {
                return null;
            }
            return ReflectionUtil.transToList(o);
        };
        Object object = Proxy.newProxyInstance(SingleLoader.class.getClassLoader(), new Class[]{SingleLoader.class}, invocationHandler);
        return (SingleLoader<?>) object;
    }

    public static SingleLoader<?> createObjectCacheSingleLoader(final Method method) {
        InvocationHandler invocationHandler = (proxy, method1, args) -> {
            if (((InvocationContext) args[0]).getMethodInvoker() == null) {
                method.setAccessible(true);
                return method.invoke(((InvocationContext) args[0]).getTarget(), ((InvocationContext) args[0]).getArgs());
            }
            return ((InvocationContext) args[0]).getMethodInvoker().get();
        };
        Object object = Proxy.newProxyInstance(SingleLoader.class.getClassLoader(), new Class[]{SingleLoader.class}, invocationHandler);
        return (SingleLoader<?>) object;
    }

    @SuppressWarnings("rawtypes")
    public static MultipleLoader<?> createObjectCacheMultiLoader(final Method method) {
        InvocationHandler invocationHandler = (proxy, method1, args) -> {
            List<InvocationContext> contexts = (List<InvocationContext>) args[0];
            // should not be null!!
            Object target = contexts.get(0).getTarget();

            // the arguments here for example may be [(1,"s"),(2,"s")]
            List<Object[]> flattenedArgs = new ArrayList<>();
            for (InvocationContext context : contexts) {
                flattenedArgs.add(context.getArgs());
            }

            // the roughened arguments here for example is parsed to [([1,2],"s")]
            Object[] roughenedArgs = ReflectionUtil.squeezeArgs(method, flattenedArgs);
            method.setAccessible(true);
            Object result = method.invoke(target, roughenedArgs);

            // check if result size is the same with argument size
            List resultList = (List) ReflectionUtil.transToList(result);

            Map<InvocationContext, Object> map = new HashMap<>();
            if (CollectionUtils.isEmpty(resultList)) {
                return map;
            }

            // check if @MapTo is declared and then resolve it
            Map<Integer, Annotation> indexedMapTo = ReflectionUtil.getIndexedMethodParameterCacheAnnotations(method);
            if (CommonUtils.isEmpty(indexedMapTo)) {
                throw new CacheOperationException("The parameter of method declaring @CacheMulti should declare @MapTo.");
            }

            // gather all arguments declaring @MapTo to MappedArguments, and map it to context
            Map<MappedArguments, InvocationContext> contextMap = new HashMap<>();
            List<Map.Entry<Integer, Annotation>> indexedMapToEntries = new ArrayList<>(indexedMapTo.entrySet());
            for (int i = 0; i < flattenedArgs.size(); i++) {
                Object[] flattenedArg = flattenedArgs.get(i);
                Object[] mappedArgs = new Object[indexedMapTo.size()];

                for (int i1 = 0; i1 < indexedMapToEntries.size(); i1++) {
                    int offset = indexedMapToEntries.get(i1).getKey();
                    mappedArgs[i1] = flattenedArg[offset];
                }
                // contexts size is the same with flattened arguments
                contextMap.put(new MappedArguments(mappedArgs), contexts.get(i));
            }

            for (Object element : resultList) {
                if (element == null) {
                    continue;
                }
                Object[] mappedArgs = new Object[indexedMapTo.size()];
                for (int i1 = 0; i1 < indexedMapToEntries.size(); i1++) {
                    MapTo mapTo = (MapTo) indexedMapToEntries.get(i1).getValue();
                    Object arg = new SpringElUtil.SpringELEvaluationContext(mapTo.value()).addVar("result$each", element).getValue();
                    mappedArgs[i1] = arg;
                }

                if (!contextMap.containsKey(new MappedArguments(mappedArgs))) {
                    // noise data
                    map.put(null, new Object());
                }

                map.put(contextMap.get(new MappedArguments(mappedArgs)), element);
            }

            return map;
        };
        Object object = Proxy.newProxyInstance(MultipleLoader.class.getClassLoader(), new Class[]{MultipleLoader.class}, invocationHandler);
        return (MultipleLoader<?>) object;
    }


    static class MappedArguments {
        private final Object[] objects;

        public MappedArguments(Object[] objects) {
            this.objects = objects;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MappedArguments)) {
                return false;
            }
            MappedArguments that = (MappedArguments) o;
            return Arrays.equals(objects, that.objects);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(objects);
        }
    }

    public static SingleLoader<?> resolveSingleLoader(Method declaredMethod) {
        return createObjectCacheSingleLoader(declaredMethod);
    }

    public static MultipleLoader<?> resolveMultiLoader(Method declaredMethod) {
        // return type should be array or collection
        if (!ReflectionUtil.isCollectionOrArray(declaredMethod.getReturnType())) {
            throw new CacheBeanParsingException("The return type of method declaring @MultipleCacheable should be an array or collection!");
        }

        return CacheResolver.createObjectCacheMultiLoader(declaredMethod);
    }

}
