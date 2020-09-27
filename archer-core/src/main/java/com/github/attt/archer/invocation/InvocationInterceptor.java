package com.github.attt.archer.invocation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.github.attt.archer.CacheManager;
import com.github.attt.archer.exception.FallbackException;
import com.github.attt.archer.metadata.EvictionMetadata;
import com.github.attt.archer.metadata.ObjectCacheMetadata;
import com.github.attt.archer.metadata.api.AbstractCacheMetadata;
import com.github.attt.archer.operation.EvictionOperation;
import com.github.attt.archer.operation.api.AbstractCacheOperation;
import com.github.attt.archer.processor.ListProcessor;
import com.github.attt.archer.processor.api.AbstractProcessor;
import com.github.attt.archer.processor.context.InvocationContext;
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
        CacheContext.setCacheManager(manager);
        INSTANCE.manager = manager;
    }

    public Object invoke(Object proxy, Object target, Supplier<?> methodInvoker, Method method, Object[] args) throws Throwable {

        // store proxy object in current thread
        CacheContext.setCurrentProxy(proxy);

        // get the actual method of target object
        method = target.getClass().getMethod(method.getName(), method.getParameterTypes());

        if (!ReflectionUtil.findAnyCacheAnnotation(method)) {
            // find if any cache annotation declared with method or its interface/super class exists
            return methodInvoker.get();
        }

        String methodSignature = ReflectionUtil.getSignature(method, true, true);

        // cache acceptation operation sources
        List<AbstractCacheOperation> cacheOperations = manager.getCacheOperations(methodSignature, AbstractCacheOperation.class);

        // cache eviction operation sources
        List<EvictionOperation> evictionOperations = manager.getEvictionOperations(methodSignature);

        // merge cache context
        AcceptationContext acceptationContext = new AcceptationContext();
        for (AbstractCacheOperation cacheOperation : cacheOperations) {
            // cache config metadata
            AbstractCacheMetadata metadata = cacheOperation.getMetadata();

            // service cache
            AbstractProcessor processor = manager.getProcessor(cacheOperation);

            if (conditionPass(metadata.getCondition(), target, method, args)) {
                logger.debug("Condition check passed, enter service cache procedure");

                acceptationContext.merge(
                        metadata instanceof ObjectCacheMetadata && ((ObjectCacheMetadata) metadata).isMultiple() ?
                                multiCacheProcessor(processor, cacheOperation, target, method, args, methodInvoker) :
                                cacheProcessor(processor, cacheOperation, target, method, args, methodInvoker)
                );
            }
        }

        // merge eviction context
        EvictionContext evictionContext = new EvictionContext();
        for (EvictionOperation evictionOperation : evictionOperations) {
            evictionContext.merge(
                    evictionProcessor(manager, evictionOperation, target, method, args)
            );
        }

        // init invoked event & get observer

        // proceed eviction pre-operations
        for (Supplier<?> preOperation : evictionContext.preOperations) {
            preOperation.get();
        }

        Object result = acceptationContext.proceed(methodInvoker);

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
     * @param cacheProcessor
     * @param cacheOperation
     * @param target
     * @param method
     * @param args
     * @param methodInvoker
     * @return
     * @throws Throwable
     */
    private AcceptationContext multiCacheProcessor(AbstractProcessor cacheProcessor,
                                                   AbstractCacheOperation cacheOperation,
                                                   Object target, Method method, Object[] args, Supplier<?> methodInvoker) throws Throwable {
        AcceptationContext acceptationContext = new AcceptationContext();

        // to gather all invocation contexts
        List<InvocationContext> contexts = new ArrayList<>();

        // stretch all array-like arguments
        List<Object[]> newArgs = ReflectionUtil.stretchArgs(args);

        for (Object[] newArg : newArgs) {
            InvocationContext context = new InvocationContext(target, method, newArg, methodInvoker);
            contexts.add(context);
        }

        push(acceptationContext.operations, () -> {
            try {
                Map all = cacheProcessor.getAll(contexts, cacheOperation);
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
        return acceptationContext;
    }

    /**
     * Create cache context which contains intercepted cache operations
     *
     * @param cacheProcessor
     * @param operationSource
     * @param target
     * @param method
     * @param args
     * @param methodInvoker
     * @return
     * @throws Throwable
     */
    private AcceptationContext cacheProcessor(AbstractProcessor cacheProcessor,
                                              AbstractCacheOperation cacheOperation,
                                              Object target, Method method, Object[] args, Supplier<?> methodInvoker) throws Throwable {
        AcceptationContext acceptationContext = new AcceptationContext();
        InvocationContext context = new InvocationContext(target, method, args, methodInvoker);

        push(acceptationContext.operations, () -> {
            try {
                Object result = cacheProcessor.get(context, cacheOperation);
                if (result != null && cacheProcessor instanceof ListProcessor) {
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
        return acceptationContext;
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
     * {@link com.github.attt.archer.processor.api.Processor#delete(Object, Object)}
     * or {@link com.github.attt.archer.processor.api.Processor#deleteAll(Object)}
     * or {@link com.github.attt.archer.processor.api.Processor#deleteAll(List, Object)}
     * </ul>
     *
     * @param management
     * @param evictionOperation
     * @param target
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    private EvictionContext evictionProcessor(CacheManager management,
                                              EvictionOperation evictionOperation,
                                              Object target, Method method, Object[] args) throws Throwable {

        EvictionContext evictionContext = new EvictionContext();
        EvictionMetadata evictionMetadata = evictionOperation.getMetadata();
        List<Supplier<?>> operations = evictionMetadata.getAfterInvocation() ? evictionContext.postOperations : evictionContext.preOperations;
        Collection<AbstractProcessor> processors = management.getProcessors();
        for (AbstractProcessor processor : processors) {
            Supplier eviction = null;
            if (evictionMetadata.getAll()) {
                // delete all without certain keys but with area
                eviction = () -> {
                    logger.debug("Delete all caches in area {}", evictionMetadata.getArea());
                    processor.deleteAll(evictionOperation);
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
                        processor.deleteAll(contexts, evictionOperation);
                        return null;
                    };
                } else {
                    InvocationContext context = new InvocationContext(target, method, args, null);
                    eviction = () -> {
                        logger.debug("Delete context: {}", context);
                        processor.delete(context, evictionOperation);
                        return null;
                    };
                }
            }

            // always delete list cache first, because complex cache may dose not just delete key, also need cache key to find some other records
            push(operations, eviction, processor instanceof ListProcessor);
        }
        return evictionContext;
    }


    /**
     * Push operation to operations chain
     *
     * @param operations
     * @param operation
     * @param highPriority true if operation needs to executed in first priority
     */
    private void push(List<Supplier<?>> operations, Supplier<?> operation, boolean highPriority) {
        if (highPriority) {
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
     * Acceptation context
     * <p>
     * To merge all accept operations
     */
    static class AcceptationContext {
        List<Supplier<?>> operations = new ArrayList<>();

        void merge(AcceptationContext acceptationContext) {
            operations.addAll(acceptationContext.operations);
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
