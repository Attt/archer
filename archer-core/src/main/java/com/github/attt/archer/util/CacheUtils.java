package com.github.attt.archer.util;

import com.github.attt.archer.cache.annotation.ArcherCache;
import com.github.attt.archer.cache.annotation.CacheMapping;
import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.exception.CacheOperationException;
import com.github.attt.archer.loader.MultipleLoader;
import com.github.attt.archer.loader.SingleLoader;
import com.github.attt.archer.metadata.ClassCacheMetadata;
import com.github.attt.archer.metadata.api.AbstractCacheMetadata;
import com.github.attt.archer.processor.context.InvocationContext;
import com.github.attt.archer.util.resolver.AnnotationResolver;
import com.github.attt.archer.util.resolver.CacheType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Cache utils
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheUtils {

    private static final Logger logger = LoggerFactory.getLogger(CacheUtils.class);

    /**
     * Resolve cache annotation to metadata
     *
     * @param method
     * @param annotation
     * @return
     * @see AbstractCacheMetadata
     */
    public static List<AbstractCacheMetadata> resolveMetadata(Method method, Annotation annotation) {
        Class<?> methodClass = method.getDeclaringClass();
        ArcherCache archerCache = ReflectionUtil.getCacheAnnotation(methodClass, ArcherCache.class);
        return resolveMetadata(archerCache, method, annotation);
    }

    /**
     * Resolve cache annotation to metadata with Service class annotation {@link ArcherCache}
     * <p>
     * Brief:
     * <ul>
     *     <li>Resolve every method cache with type defined in {@link CacheType} with its resolver
     *     (implements {@link AnnotationResolver})
     *     <li>Create metadata (implements {@link AbstractCacheMetadata})
     * </ul>
     * <p>
     * About {@link ArcherCache} :
     * <ul>
     *     <li>Use {@link ArcherCache#region()},{@link ArcherCache#valueSerializer()},{@link ArcherCache#keyGenerator()} as
     *     default value for every method cache of the same service class
     *     <li>Use {@link ArcherCache#prefix()} to prepend prefix string to every cache key of method cache of the same
     *     service class
     * </ul>
     *
     * @param archerCache
     * @param method
     * @param annotation
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<AbstractCacheMetadata> resolveMetadata(ArcherCache archerCache, Method method, Annotation annotation) {

        ClassCacheMetadata classCacheMetadata = new ClassCacheMetadata();
        classCacheMetadata.setKeyPrefix(
                CommonUtils.escapeNullable(archerCache, () -> CommonUtils.isNotEmpty(archerCache.prefix()) ? archerCache.prefix() : archerCache.value(), "")
        );
        classCacheMetadata.setValueSerializer(
                CommonUtils.escapeNullable(archerCache, () -> archerCache.valueSerializer(), "")
        );
        classCacheMetadata.setKeyGenerator(
                CommonUtils.escapeNullable(archerCache, () -> archerCache.keyGenerator(), "")
        );
        classCacheMetadata.setArea(
                CommonUtils.escapeNullable(archerCache, () -> archerCache.region(), "")
        );


        AnnotationResolver resolver = CacheType.resolve(annotation).getResolver();
        if (resolver == null) {
            throw new CacheBeanParsingException("Unsupported cache annotation : " + annotation);
        }

        return resolver.resolve(method, classCacheMetadata, annotation);
    }

    /**
     * Get actual return type of method
     * <p>
     * Actual return type depends on whether method return type is array-like or not, which means:
     * <ul>
     *     <li> Type of each of array element
     *     <li> Type of each of collection's component
     *     <li> Type of other java object
     * </ul>
     *
     * @param method
     * @return
     */
    public static Type parseCacheEntityType(Method method) {
        Type type = ReflectionUtil.getComponentOrArgumentType(method);
        if (type == null) {
            throw new RuntimeException("Listable/Multiple cache class (return type) should be collection or array");
        }
        return type;
    }

    /**
     * Create loader for 'the elements part' of {@link com.github.attt.archer.cache.annotation.CacheList}
     * <p>
     * The loader is used when missing cache, this mainly:
     * <ul>
     *     <li> Invoke method defined in {@link InvocationContext}
     *     <li> Reform result to {@link List} type whether the original method return type
     *     is {@link List} or {@link Set} or array. (In {@link com.github.attt.archer.invocation.InvocationInterceptor}
     *     interception procedure, the result of this loader and of cache hitting will be
     *     treated as {@link List} consistently.)
     * </ul>
     *
     * @return
     */
    public static SingleLoader<?> createListableCacheLoader() {
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            Object o = ((InvocationContext) args[0]).getMethodInvoker().get();
            if (o == null) {
                return null;
            }
            return ReflectionUtil.transToList(o);
        };
        Object object = Proxy.newProxyInstance(SingleLoader.class.getClassLoader(), new Class[]{SingleLoader.class}, invocationHandler);
        return (SingleLoader<?>) object;
    }

    /**
     * Create loader for {@link com.github.attt.archer.cache.annotation.Cache} and 'the list part' of
     * {@link com.github.attt.archer.cache.annotation.CacheList}
     * <p>
     * Simply invoke method defined in {@link InvocationContext}
     *
     * @param method
     * @return
     */
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

    /**
     * Create loader for {@link com.github.attt.archer.cache.annotation.Cache} which {@link com.github.attt.archer.cache.annotation.Cache#asOne()} is false
     * <p>
     * Load all data whether all caches are missing or part of caches is missing:
     * <ul>
     *     <li> Squeeze arguments defined in {@link InvocationContext} list
     *     <li> Invoke method defined in {@link InvocationContext} with squeezed arguments
     *     <li> Map every {@link InvocationContext} to every result with {@link com.github.attt.archer.cache.annotation.CacheMapping} (keep the order right)
     * </ul>
     * <p>
     * In multi result cache case, there will happen 'data noise' or data missing (see the comment of {@link com.github.attt.archer.cache.annotation.CacheMapping} about 'noise data').
     * To resolve 'data noise' or data missing problem, any cache of any element of {@link InvocationContext} list missing will
     * cause the whole {@link InvocationContext} list data reloading but not just reload the absent ones.
     * <p>
     * Correctness is higher priority than efficiency
     *
     * @param method
     * @return
     * @see ReflectionUtil#stretchArgs(Object[])
     * @see ReflectionUtil#squeezeArgs(Method, List)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MultipleLoader<?> createObjectCacheMultiLoader(final Method method) {
        InvocationHandler invocationHandler = (proxy, method1, args) -> {
            List<InvocationContext> contexts = (List<InvocationContext>) args[0];
            // should not be null!!
            Object target = contexts.get(0).getTarget();

            // the arguments here for example may be [(1,"s"),(2,"s")]
            List<Object[]> stretchedArgs = new ArrayList<>();
            for (InvocationContext context : contexts) {
                stretchedArgs.add(context.getArgs());
            }

            // the squeezed arguments here for example is parsed to [([1,2],"s")]
            Object[] squeezedArgs = ReflectionUtil.squeezeArgs(method, stretchedArgs);
            method.setAccessible(true);
            Object result = method.invoke(target, squeezedArgs);

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
            for (int i = 0; i < stretchedArgs.size(); i++) {
                Object[] stretchedArg = stretchedArgs.get(i);
                Object[] mappedArgs = new Object[indexedMapTo.size()];

                for (int i1 = 0; i1 < indexedMapToEntries.size(); i1++) {
                    int offset = indexedMapToEntries.get(i1).getKey();
                    mappedArgs[i1] = stretchedArg[offset];
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
                    CacheMapping mapTo = (CacheMapping) indexedMapToEntries.get(i1).getValue();
                    String expression = "#result$each." + mapTo.toResult();
                    if (CommonUtils.isEmpty(mapTo.toResult())) {
                        expression = "#result$each";
                    }
                    Object arg = new SpringElUtil.SpringELEvaluationContext(expression).addVar("result$each", element).getValue();
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

    /**
     * Used for {@link Map}'s key check
     * <p>
     * Override {@link Object#equals(Object)} and {@link Object#hashCode()}
     * to make that the map key is the same represents every single argument
     * is the same.
     */
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

        return CacheUtils.createObjectCacheMultiLoader(declaredMethod);
    }

}
