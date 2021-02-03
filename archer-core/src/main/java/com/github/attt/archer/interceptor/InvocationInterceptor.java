package com.github.attt.archer.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.github.attt.archer.CacheManager;
import com.github.attt.archer.exception.FallbackException;
import com.github.attt.archer.annotation.metadata.EvictionMetadata;
import com.github.attt.archer.annotation.metadata.ObjectCacheMetadata;
import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;
import com.github.attt.archer.annotation.config.EvictionProperties;
import com.github.attt.archer.annotation.config.AbstractCacheProperties;
import com.github.attt.archer.invoker.Invoker;
import com.github.attt.archer.invoker.ListCacheInvoker;
import com.github.attt.archer.invoker.AbstractInvoker;
import com.github.attt.archer.invoker.context.InvocationContext;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.ReflectionUtil;
import com.github.attt.archer.util.SpringElUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

/**
 * Invocation interceptor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@SuppressWarnings("all")
public class InvocationInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(InvocationInterceptor.class);

    private CacheManager manager;

    public static final InvocationInterceptor INSTANCE = new InvocationInterceptor();

    InvocationInterceptor() {
    }

    public static void init(CacheManager manager) {
        ManualCacheHandler.setCacheManager(manager);
        INSTANCE.manager = manager;
    }

    public Object invoke(Object proxy, Object target, Supplier<?> methodInvoker, Method method, Object[] args) throws Throwable {

        // store proxy object in current thread
        ManualCacheHandler.setCurrentProxy(proxy);

        // get the actual method of target object
        method = target.getClass().getMethod(method.getName(), method.getParameterTypes());

        if (!ReflectionUtil.findAnyCacheAnnotation(method)) {
            // find if any cache annotation declared with method or its interface/super class exists
            return methodInvoker.get();
        }

        String methodSignature = ReflectionUtil.getSignature(method, true, true);

        // cache properties
        List<AbstractCacheProperties> cachePropertiesList = manager.getCacheProperties(methodSignature, AbstractCacheProperties.class);

        // cache eviction properties
        List<EvictionProperties> evictionPropertiesList = manager.getEvictionProperties(methodSignature);

        // merge cache context
        CacheContext cacheContext = new CacheContext();
        for (AbstractCacheProperties cacheProperties : cachePropertiesList) {
            // cache config metadata
            AbstractCacheMetadata metadata = cacheProperties.getMetadata();

            // service cache
            AbstractInvoker processor = manager.getProcessor(cacheProperties);

            if (conditionPass(metadata.getCondition(), target, method, args)) {
                logger.debug("Condition check passed, enter service cache procedure");

                cacheContext.merge(
                        metadata instanceof ObjectCacheMetadata && ((ObjectCacheMetadata) metadata).isMultiple() ?
                                pushMultiCacheInvoker(processor, cacheProperties, target, method, args, methodInvoker) :
                                pushCacheInvoker(processor, cacheProperties, target, method, args, methodInvoker)
                );
            }
        }

        // merge eviction context
        EvictionContext evictionContext = new EvictionContext();
        for (EvictionProperties evictionProperties : evictionPropertiesList) {
            evictionContext.merge(
                    pushEvictionInvoker(manager, evictionProperties, target, method, args)
            );
        }

        // init invoked event & get observer

        // proceed eviction pre-operations
        for (Supplier<?> preOperation : evictionContext.preOperations) {
            preOperation.get();
        }

        Object result = cacheContext.proceed(methodInvoker);

        // proceed eviction post-operations
        for (Supplier<?> postOperation : evictionContext.postOperations) {
            postOperation.get();
        }
        return result;
    }

    /**
     * Check SpringEL Expression formed condition
     *
     * @param condition
     * @param target
     * @param method
     * @param args
     * @return true if pass
     */
    private boolean conditionPass(String condition, Object target, Method method, Object[] args) {
        if (!CommonUtils.isEmpty(condition)) {
            return SpringElUtil.parse(condition).setMethodInvocationContext(target, method, args, null).getValue(Boolean.class);
        }
        return true;
    }

    /**
     * Create cache context which contains intercepted cache operations
     *
     * @param invoker
     * @param cacheProperties
     * @param target
     * @param method
     * @param args
     * @param methodInvoker
     * @return
     * @throws Throwable
     */
    private CacheContext pushMultiCacheInvoker(AbstractInvoker invoker,
                                               AbstractCacheProperties cacheProperties,
                                               Object target, Method method, Object[] args, Supplier<?> methodInvoker) throws Throwable {
        CacheContext cacheContext = new CacheContext();

        // to gather all invocation contexts
        List<InvocationContext> contexts = new ArrayList<>();

        // stretch all array-like arguments
        List<Object[]> newArgs = ReflectionUtil.stretchArgs(args);

        for (Object[] newArg : newArgs) {
            InvocationContext context = new InvocationContext(target, method, newArg, methodInvoker);
            contexts.add(context);
        }

        push(cacheContext.operations, () -> {
            try {
                Map all = invoker.getAll(contexts, cacheProperties);
                Object[] values = all.values().toArray();
                if (all instanceof LinkedHashMap) {
                    // if implementation is linked map, make sure result order is correct
                    Object[] keys = all.keySet().toArray(new InvocationContext[0]);
                    values = new Object[keys.length];
                    for (int i = 0; i < keys.length; i++) {
                        values[i] = all.get(keys[i]);
                    }
                }
                ReflectionUtil.makeAccessible(values.getClass().getComponentType());
                return JSON.parseObject(JSON.toJSONString(values), method.getGenericReturnType(), new ParserConfig(true), Feature.SupportNonPublicField);
            } catch (FallbackException fallbackException) {
                return methodInvoker.get();
            } finally {
            }
        }, false);
        return cacheContext;
    }

    /**
     * Create cache context which contains intercepted cache operations
     *
     * @param invoker
     * @param cacheProperties
     * @param target
     * @param method
     * @param args
     * @param methodInvoker
     * @return
     * @throws Throwable
     */
    private CacheContext pushCacheInvoker(AbstractInvoker invoker,
                                          AbstractCacheProperties cacheProperties,
                                          Object target, Method method, Object[] args, Supplier<?> methodInvoker) throws Throwable {
        CacheContext cacheContext = new CacheContext();
        InvocationContext context = new InvocationContext(target, method, args, methodInvoker);

        push(cacheContext.operations, () -> {
            try {
                Object result = invoker.get(context, cacheProperties);
                if (result != null && invoker instanceof ListCacheInvoker) {
                    // fix real type if result is array-like
                    Type componentOrArgumentType = ReflectionUtil.getComponentOrArgumentType(method);
                    if (componentOrArgumentType != null) {
                        ReflectionUtil.makeAccessible(componentOrArgumentType);
                    }
                    result = JSON.parseObject(JSON.toJSONString(result), method.getGenericReturnType(), new ParserConfig(true), Feature.SupportNonPublicField);
                }
                return result;
            } catch (FallbackException fallbackException) {
                return methodInvoker.get();
            } finally {
            }
        }, false);
        return cacheContext;
    }

    /**
     * Create eviction context which contains intercepted eviction operations
     * <p>
     * Eviction operation only holds metadata definitions, but no processor to accept it.
     * To do eviction:
     * <ul>
     * <li> Iterate all other non-evict operations
     * <li> Check area
     * <li> Create {@link InvocationContext}
     * <li> Evict caches using non-evict operation's
     * {@link Invoker#delete(Object, Object)}
     * or {@link Invoker#deleteAll(Object)}
     * or {@link Invoker#deleteAll(List, Object)}
     * </ul>
     *
     * @param management
     * @param evictionProperties
     * @param target
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    private EvictionContext pushEvictionInvoker(CacheManager management,
                                                EvictionProperties evictionProperties,
                                                Object target, Method method, Object[] args) throws Throwable {

        EvictionContext evictionContext = new EvictionContext();
        EvictionMetadata evictionMetadata = evictionProperties.getMetadata();
        List<Supplier<?>> operations = evictionMetadata.getAfterInvocation() ? evictionContext.postOperations : evictionContext.preOperations;
        Collection<AbstractInvoker> invokers = management.getInvokers();
        for (AbstractInvoker invoker : invokers) {
            Supplier eviction = null;
            if (evictionMetadata.getAll()) {
                // delete all without certain keys but with region
                eviction = () -> {
                    logger.debug("Delete all caches in region {}", evictionMetadata.getRegion());
                    invoker.deleteAll(evictionProperties);
                    return null;
                };
            } else {
                if (evictionMetadata.getMultiple()) {
                    List<InvocationContext> contexts = new ArrayList<>();
                    List<Object[]> newArgs = ReflectionUtil.stretchArgs(args);
                    for (Object[] newArg : newArgs) {
                        InvocationContext context = new InvocationContext(target, method, newArg, null);
                        contexts.add(context);
                    }
                    eviction = () -> {
                        logger.debug("Delete contexts: {}", contexts);
                        invoker.deleteAll(contexts, evictionProperties);
                        return null;
                    };
                } else {
                    InvocationContext context = new InvocationContext(target, method, args, null);
                    eviction = () -> {
                        logger.debug("Delete context: {}", context);
                        invoker.delete(context, evictionProperties);
                        return null;
                    };
                }
            }

            // always delete list cache first, because complex cache may dose not just delete key, also need cache key to find some other records
            push(operations, eviction, invoker instanceof ListCacheInvoker);
        }
        return evictionContext;
    }


    /**
     * Push operation to operations chain
     *
     * @param operations
     * @param operation
     * @param higherPriority true if operation needs to executed in first priority
     */
    private void push(List<Supplier<?>> operations, Supplier<?> operation, boolean higherPriority) {
        if (higherPriority) {
            operations.add(0, operation);
        } else {
            operations.add(operation);
        }
    }


    /**
     * Eviction context
     * <p>
     * To merge all evict operations befor or after invoking method
     */
    static class EvictionContext {
        List<Supplier<?>> preOperations = new ArrayList<>();
        List<Supplier<?>> postOperations = new ArrayList<>();

        void merge(EvictionContext evictionContext) {
            preOperations.addAll(evictionContext.preOperations);
            postOperations.addAll(evictionContext.postOperations);
        }
    }

    /**
     * Cache context
     * <p>
     * To merge all accept operations
     */
    static class CacheContext {
        List<Supplier<?>> operations = new ArrayList<>();

        void merge(CacheContext cacheContext) {
            operations.addAll(cacheContext.operations);
        }

        Object proceed(Supplier<?> methodInvoker) {
            if (CommonUtils.isEmpty(operations)) {
                return methodInvoker.get();
            }
            Object result = null;
            for (Supplier<?> operation : operations) {
                result = operation.get();
            }
            return result;
        }
    }
}
