package com.github.attt.archer.util;

import com.alibaba.fastjson.JSON;
import com.github.attt.archer.annotation.*;
import com.github.attt.archer.annotation.extra.MapTo;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection util
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ReflectionUtil extends Reflections {

    private static final Logger log = LoggerFactory.getLogger(ReflectionUtil.class);


    @SuppressWarnings("unchecked")
    private static final Class<Annotation>[] CACHE_METHOD_PARAMETER_ANNOTATIONS = new Class[]{
            MapTo.class
    };

    @SuppressWarnings("unchecked")
    private static final Class<Annotation>[] OBJECT_CACHE_METHOD_ANNOTATIONS = new Class[]{
            Cache.class, CacheMulti.class
    };

    @SuppressWarnings("unchecked")
    private static final Class<Annotation>[] LIST_CACHE_METHOD_ANNOTATIONS = new Class[]{
            CacheList.class
    };

    @SuppressWarnings("unchecked")
    private static final Class<Annotation>[] EVICT_CACHE_METHOD_ANNOTATIONS = new Class[]{
            Evict.class, Evicts.class, EvictMulti.class, EvictMultis.class
    };


    @SuppressWarnings("unchecked")
    private static final Class<Annotation>[][] CACHE_METHOD_ANNOTATIONS = new Class[][]{
            OBJECT_CACHE_METHOD_ANNOTATIONS, LIST_CACHE_METHOD_ANNOTATIONS, EVICT_CACHE_METHOD_ANNOTATIONS
    };

    @SuppressWarnings("unchecked")
    private static final Class<Annotation>[] CACHE_CLASS_ANNOTATIONS = new Class[]{
            Cacheable.class
    };

    /**
     * method signature -> parameter index -> annotation
     */
    private static final Map<String, Map<Integer, Annotation>> METHOD_PARAMETER_ANNOTATIONS = new HashMap<>();

    /**
     * type name -> interface or abstract class
     */
    private static final Map<String, Class> ABSTRACT_ANNOTATED_TYPE_CLASS = new HashMap<>();

    /**
     * type name -> class
     */
    private static final Map<String, Class> ANNOTATED_TYPE_CLASS = new HashMap<>();

    /**
     * type name -> annotation
     */
    private static final Map<String, Annotation> TYPE_ANNOTATIONS = new HashMap<>();

    /**
     * method signature -> interface or abstract method
     */
    private static final Map<String, List<Method>> ABSTRACT_ANNOTATED_METHOD_CLASS = new HashMap<>();

    /**
     * method signature -> method
     */
    private static final Map<String, Method> ANNOTATED_METHOD_CLASS = new HashMap<>();

    /**
     * method signature -> method annotations
     */
    private static final Map<String, List<Annotation>> METHOD_ANNOTATIONS = new HashMap<>();

    /**
     * repeatable annotation container -> method signature
     */
    private static final Map<Class, String> REPEATABLE_ANNOTATIONS_CONTAINER_METHOD = new HashMap<>();


    private static ReflectionUtil reflections;


    public static void forPackage(String... basePackage) {
        if (basePackage.length == 0) {
            basePackage = new String[]{"."};
        }
        reflections = new ReflectionUtil (
                new ConfigurationBuilder()
                        .addClassLoader(ReflectionUtil.class.getClassLoader())
                        .forPackages(basePackage)
                        .addScanners(new SubTypesScanner())
                        .addScanners(new TypeAnnotationsScanner())
                        .addScanners(new MethodAnnotationsScanner())
                        .addScanners(new FieldAnnotationsScanner())
                        .addScanners(new MethodParameterScanner())
        );
        init();
    }

    public ReflectionUtil(Configuration configuration) {

        super(configuration);
        try {
            // to fix scanner issue in reflections with version 0.9.12
            Field storeMapField = store.getClass().getDeclaredField("storeMap");
            storeMapField.setAccessible(true);
            ConcurrentHashMap<String, Map<String, Collection<String>>> storeMap = (ConcurrentHashMap<String, Map<String, Collection<String>>>) storeMapField.get(store);
            storeMap.computeIfAbsent(SubTypesScanner.class.getSimpleName(), s -> new ConcurrentHashMap<>());
            storeMap.computeIfAbsent(TypeAnnotationsScanner.class.getSimpleName(), s -> new ConcurrentHashMap<>());
            storeMap.computeIfAbsent(MethodAnnotationsScanner.class.getSimpleName(), s -> new ConcurrentHashMap<>());
            storeMap.computeIfAbsent(FieldAnnotationsScanner.class.getSimpleName(), s -> new ConcurrentHashMap<>());
        } catch (Throwable t) {
            log.error("Can't initialize ReflectionUtil");
        }
    }

    private static void init() {

        // init types
        for (Class<Annotation> annotationClass : CACHE_CLASS_ANNOTATIONS) {
            Set<Class<?>> annotatedTypes = reflections.getTypesAnnotatedWith(annotationClass, true);
            for (Class type : annotatedTypes) {
                String signature = type.getName();
                Annotation annotation = type.getAnnotation(annotationClass);
                TYPE_ANNOTATIONS.put(signature, annotation);
                if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                    ABSTRACT_ANNOTATED_TYPE_CLASS.put(signature, type);
                } else {
                    ANNOTATED_TYPE_CLASS.put(signature, type);
                }
            }
        }

        // init methods
        for (Class<Annotation>[] cacheMethodAnnotations : CACHE_METHOD_ANNOTATIONS) {
            for (Class<Annotation> annotationClass : cacheMethodAnnotations) {
                Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(annotationClass);
                for (Method method : annotatedMethods) {
                    Class<?> declaringClass = method.getDeclaringClass();
                    String signature = getSignature(method, true, true);
                    Annotation annotation = method.getAnnotation(annotationClass);
                    METHOD_ANNOTATIONS.compute(signature, (key, annotations) -> {
                        if (annotations == null) {
                            annotations = new ArrayList<>();
                        }
                        annotations.add(annotation);
                        return annotations;
                    });

                    if (declaringClass.isInterface() || Modifier.isAbstract(declaringClass.getModifiers())) {
                        signature = getSignature(method, false, true);
                        ABSTRACT_ANNOTATED_METHOD_CLASS.compute(signature, (sig, methods) -> {
                            if (methods == null) {
                                methods = new ArrayList<>();
                            }
                            methods.add(method);
                            return methods;
                        });
                    } else {
                        ANNOTATED_METHOD_CLASS.put(signature, method);
                    }
                }

                // collect repeatable annotations
                Repeatable repeatable = annotationClass.getDeclaredAnnotation(Repeatable.class);
                if (repeatable != null) {
                    for (Method declaredMethod : repeatable.value().getDeclaredMethods()) {
                        if (declaredMethod.getReturnType().isArray()
                                && declaredMethod.getReturnType().getComponentType().equals(annotationClass)) {
                            REPEATABLE_ANNOTATIONS_CONTAINER_METHOD.put(repeatable.value(), declaredMethod.getName());
                        }
                    }
                }
            }
        }

        // init method parameters
        for (Class<Annotation> annotationClass : CACHE_METHOD_PARAMETER_ANNOTATIONS) {
            Set<Method> annotatedMethods = reflections.getMethodsWithAnyParamAnnotated(annotationClass);
            for (Method method : annotatedMethods) {
                String signature = getSignature(method, true, true);
                for (int i = 0; i < method.getParameters().length; i++) {
                    Parameter parameter = method.getParameters()[i];
                    Annotation annotation = parameter.getDeclaredAnnotation(annotationClass);
                    if (annotation == null) {
                        continue;
                    }
                    int finalI = i;
                    METHOD_PARAMETER_ANNOTATIONS.compute(signature, (key, indexedAnnotations) -> {
                        if (indexedAnnotations == null) {
                            indexedAnnotations = new HashMap<>();
                        }
                        indexedAnnotations.put(finalI, annotation);
                        return indexedAnnotations;
                    });
                }
            }
        }

    }

    /**
     * If class-A is declared by abstract class or interface
     * and class-B implement it. When cache annotation is declared
     * only by class-A. the result will be class-A's signature when passing
     * class-B as parameter.
     *
     * @param clazz
     * @return
     */
    public static String getSignatureForCache(Class clazz) {
        String signature = clazz.getName();
        if (!ANNOTATED_TYPE_CLASS.containsKey(signature)) {
            for (Map.Entry<String, Class> entry : ABSTRACT_ANNOTATED_TYPE_CLASS.entrySet()) {
                String sig = entry.getKey();
                Class c = entry.getValue();
                boolean isSuper = c.isAssignableFrom(clazz);
                if (isSuper) {
                    return sig;
                }
            }
        }
        return signature;
    }

    /**
     * If method-A is declared by abstract class or interface
     * and method-B implement it. When cache annotation is declared
     * only by method-A. the result will be method-A's signature when passing
     * method-B as parameter.
     *
     * @param method
     * @return
     */
    public static String getSignatureForCache(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        String signature = getSignature(method, true, true);
        if (!ANNOTATED_METHOD_CLASS.containsKey(signature)) {
            String sig = getSignature(method, false, true);
            List<Method> cachedMethods = ABSTRACT_ANNOTATED_METHOD_CLASS.getOrDefault(sig, new ArrayList<>());
            for (Method cachedMethod : cachedMethods) {
                if (cachedMethod != null && cachedMethod.getDeclaringClass().isAssignableFrom(declaringClass)) {
                    return getSignature(cachedMethod, true, true);
                }
            }
        }
        return signature;
    }

    /**
     * Get cache annotation declared by class or its super class
     *
     * @param clazz
     * @return
     */
    public static Annotation getCacheAnnotation(Class clazz) {
        String signature = getSignatureForCache(clazz);
        return TYPE_ANNOTATIONS.getOrDefault(signature, null);
    }

    /**
     * Get cache annotation declared by class or its super class
     *
     * @param clazz
     * @param annotationType
     * @return
     */
    public static <T extends Annotation> T getCacheAnnotation(Class clazz, Class<T> annotationType) {
        String signature = getSignatureForCache(clazz);
        Annotation annotation = TYPE_ANNOTATIONS.getOrDefault(signature, null);
        if (annotation != null && annotation.annotationType().getName().equals(annotationType.getName())) {
            return (T) annotation;
        }
        return null;
    }

    /**
     * Get cache annotations declared by method or its super method
     *
     * @param method
     * @return
     */
    public static List<Annotation> getCacheAnnotations(Method method) {
        String signature = getSignatureForCache(method);
        return METHOD_ANNOTATIONS.getOrDefault(signature, new ArrayList<>());
    }

    /**
     * Get cache annotation declared by method or its super method
     *
     * @param method
     * @param annotationTypes
     * @return
     */
    public static List getCacheAnnotations(Method method, Class... annotationTypes) {
        String signature = getSignatureForCache(method);
        List<Annotation> annotations = METHOD_ANNOTATIONS.getOrDefault(signature, new ArrayList<>());
        List greppedAnnotations = new ArrayList<>();
        for (Annotation annotation : annotations) {
            for (Class annotationType : annotationTypes) {
                if (annotation.annotationType().getName().equals(annotationType.getName())) {
                    greppedAnnotations.add(annotation);
                    break;
                }
            }
        }
        return greppedAnnotations;
    }

    /**
     * Get repeatable cache annotation declared by method or its super method
     *
     * @param method
     * @return
     */
    public static List<Annotation> getRepeatableCacheAnnotations(Method method) {
        List<Annotation> repeatableAnnotations = new ArrayList<>();
        String signature = getSignatureForCache(method);
        List<Annotation> annotations = METHOD_ANNOTATIONS.getOrDefault(signature, new ArrayList<>());
        for (Annotation annotation : annotations) {
            if (annotation != null) {
                String containedFieldGetter = REPEATABLE_ANNOTATIONS_CONTAINER_METHOD.getOrDefault(annotation.annotationType(), null);
                if (containedFieldGetter != null) {
                    // if annotation is container
                    try {
                        Method getter = annotation.annotationType().getDeclaredMethod(containedFieldGetter);
                        getter.setAccessible(true);
                        repeatableAnnotations.addAll(Arrays.asList((Annotation[]) getter.invoke(annotation)));
                    } catch (Throwable ignored) {
                    }
                } else {
                    // check if annotation is repeatable
                    Repeatable repeatable = annotation.annotationType().getDeclaredAnnotation(Repeatable.class);
                    if (repeatable != null) {
                        repeatableAnnotations.add(annotation);
                    }
                }
            }
        }
        return repeatableAnnotations;
    }

    /**
     * Get indexed cache annotations of method parameter its super method parameter
     *
     * @param method
     * @return
     */
    public static Map<Integer, Annotation> getIndexedMethodParameterCacheAnnotations(Method method) {
        if (method == null || CommonUtils.isEmpty(method.getParameters())) {
            return new HashMap<>();
        }
        String signature = getSignatureForCache(method);
        return METHOD_PARAMETER_ANNOTATIONS.getOrDefault(signature, new HashMap<>());
    }


    /**
     * Tell which type annotation passed in is
     *
     * @param annotation
     * @return
     */
    public static CacheType typeOf(Annotation annotation) {
        if (annotation != null) {
            for (Class<Annotation> annotationClass : OBJECT_CACHE_METHOD_ANNOTATIONS) {
                if (annotationClass.getName().equals(annotation.annotationType().getName())) {
                    return CacheType.OBJECT;
                }
            }
            for (Class<Annotation> annotationClass : LIST_CACHE_METHOD_ANNOTATIONS) {
                if (annotationClass.getName().equals(annotation.annotationType().getName())) {
                    return CacheType.LIST;
                }
            }
            for (Class<Annotation> annotationClass : EVICT_CACHE_METHOD_ANNOTATIONS) {
                if (annotationClass.getName().equals(annotation.annotationType().getName())) {
                    return CacheType.EVICT;
                }
            }
        }
        return CacheType.NULL;
    }


    public enum CacheType {
        OBJECT, LIST, EVICT, NULL
    }

    /**
     * Whether there is any cache annotation bound to method
     *
     * @param method
     * @return
     */
    public static boolean findAnyCacheAnnotation(Method method) {
        return getCacheAnnotations(method).size() > 0;
    }

    public static String parametersAsString(Method method) {
        return parametersAsString(method, null, false);
    }

    public static String getSignatureWithArgValuesAndReturnType(Method method, Object[] args) {
        return "(" + method.getReturnType().getName() + ")" + method.getName() + "("
                + parametersAsString(method, args, false) + ")";
    }

    public static String getSignatureWithArgValues(Method method, Object[] args) {
        return method.getName() + "("
                + parametersAsString(method, args, false) + ")";
    }


    public static String getSignature(Method method, boolean withClass, boolean longTypeNames) {
        String classSig = withClass ? method.getDeclaringClass().getName() + "." : "";
        return classSig + method.getName() + "("
                + parametersAsString(method, null, longTypeNames) + ")";
    }


    public static String getSignature(Field field, boolean withClass) {
        String classSig = withClass ? field.getDeclaringClass().getName() + "." : "";
        return classSig + "(" + field.getType().getName() + ")" + field.getName();
    }

    public static String parametersAsString(Method method,
                                            Object[] args,
                                            boolean longTypeNames) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            return "";
        }
        StringBuilder paramString = new StringBuilder();
        if (args != null && args.length > 0) {
            paramString.append("`").append(JSON.toJSONString(args[0])).append("`");
            for (int i = 1; i < args.length; i++) {
                paramString.append(",`").append(JSON.toJSONString(args[i])).append("`");
            }
        } else {
            paramString.append(longTypeNames ? parameterTypes[0].getName()
                    : parameterTypes[0].getSimpleName());
            for (int i = 1; i < parameterTypes.length; i++) {
                paramString.append(",").append(
                        longTypeNames ? parameterTypes[i].getName()
                                : parameterTypes[i].getSimpleName());
            }
        }
        return paramString.toString();
    }

    public static String getSignature(Method method) {
        return getSignature(method, false, false);
    }


    public static void makeAccessible(Class clazz) {
        Constructor<?>[] constructors = clazz.getClass().getConstructors();
        for (Constructor<?> constructor : constructors) {
            constructor.setAccessible(true);
        }

        for (Field declaredField : clazz.getDeclaredFields()) {
            declaredField.setAccessible(true);
        }

        for (Field field : clazz.getFields()) {
            field.setAccessible(true);
        }
    }

    public static void makeAccessible(Type type) {
        Class<?> aClass = toClass(type);
        if (aClass != null) {
            makeAccessible(aClass);
        }
    }

    public static boolean isCollectionOrArray(Type type) {
        return isCollection(type) || isArray(type);
    }

    public static boolean isArray(Type type) {
        Class<?> genericClass = toClass(type);
        return genericClass.isArray();
    }

    public static boolean isCollection(Type type) {
        Class<?> genericClass = toClass(type);
        return Collection.class.isAssignableFrom(genericClass);
    }

    public static Type getComponentOrArgumentType(Method method) {
        final Class<?> returnType = method.getReturnType();
        final Type genericReturnType = method.getGenericReturnType();

        if (isCollection(returnType)) {
            ParameterizedType returnParameterizedType = (ParameterizedType) genericReturnType;
            Type valueType = returnParameterizedType.getActualTypeArguments()[0];
            return toType(valueType);

        } else if (returnType.isArray()) {
            return returnType.getComponentType();
        } else {
            return null;
        }
    }

    public static Type getComponentOrArgumentType(Type genericType) {
        final Class<?> returnType = toClass(genericType);
        if (isCollection(returnType)) {
            ParameterizedType returnParameterizedType = (ParameterizedType) genericType;
            Type valueType = returnParameterizedType.getActualTypeArguments()[0];
            return toType(valueType);
        } else if (returnType.isArray()) {
            return returnType.getComponentType();
        } else {
            return null;
        }
    }

    public static Class<?> toClass(Type type) {
        Class<?> genericClass;
        if (type instanceof ParameterizedType) {
            genericClass = (Class<?>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof TypeVariable) {
            try {
                genericClass = Class.forName(((TypeVariable) type).getGenericDeclaration().toString());
            } catch (ClassNotFoundException e) {
                return null;
            }
        } else {
            genericClass = (Class<?>) type;
        }
        return genericClass;
    }

    public static Type toType(Type type) {
        Type genericClass;
        if (type instanceof ParameterizedType) {
            genericClass = ((ParameterizedType) type).getRawType();
        } else if (type instanceof TypeVariable) {
            try {
                genericClass = Class.forName(((TypeVariable) type).getGenericDeclaration().toString());
            } catch (ClassNotFoundException e) {
                return null;
            }
        } else {
            genericClass = type;
        }
        return genericClass;
    }

    public static boolean isNumber(Class<?> type) {
        boolean primitiveNumber = type.isPrimitive() && (
                type.equals(Short.TYPE)
                        || type.equals(Float.TYPE)
                        || type.equals(Double.TYPE)
                        || type.equals(Integer.TYPE) || type.equals(Long.TYPE));
        boolean number = type.equals(Short.class)
                || type.equals(Float.class)
                || type.equals(Double.class)
                || type.equals(Integer.class)
                || type.equals(Long.class);
        return primitiveNumber || number;
    }

    public static Object transToList(Object invoke) {
        if (invoke instanceof LinkedHashSet) {
            // save order anyway
            List ol = new ArrayList();
            LinkedHashSet orderedSet = (LinkedHashSet) invoke;
            for (Object o : orderedSet) {
                ol.add(o);
            }
            return ol;
        } else if (invoke.getClass().isArray()) {
            return toList(invoke);
        } else {
            return invoke;
        }
    }

    /**
     * stretch arguments
     * <p>
     * if Object[] args is [list(1,2,3),string(a)]
     * stretched result is [1,string(a)],[2,string(a)],[3,string(a)]
     *
     * @param args
     * @return
     */
    public static List<Object[]> stretchArgs(Object[] args) {
        List<Object[]> newArgs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            List list = null;
            if (arg.getClass().isArray()) {
                list = toList(arg);
            } else if (ReflectionUtil.isCollection(arg.getClass())) {
                list = new ArrayList((Collection) arg);
            }
            if (list != null) {
                for (Object argElement : list) {
                    Object[] flatArg = new Object[args.length];
                    newArgs.add(flatArg);
                    flatArg[i] = argElement;
                }
            }
        }

        for (Object[] newArg : newArgs) {
            for (int i = 0; i < newArg.length; i++) {
                if (newArg[i] == null) {
                    newArg[i] = args[i];
                }
            }
        }
        return newArgs;
    }

    /**
     * reversal operation of {@link #stretchArgs(Object[])}
     *
     * @param method
     * @param args
     * @return
     */
    public static Object[] squeezeArgs(Method method, List<Object[]> args) {
        Object[] args1 = args.get(0);
        Object[] roughenedArgs = new Object[method.getParameterCount()];
        for (int i = 0; i < method.getGenericParameterTypes().length; i++) {
            Type genericParameterType = method.getGenericParameterTypes()[i];
            if (ReflectionUtil.isCollectionOrArray(genericParameterType)) {
                List keyArgs = new ArrayList();
                for (Object[] arg : args) {
                    keyArgs.add(arg[i]);
                }
                Object o = JSON.parseObject(JSON.toJSONString(keyArgs), genericParameterType);
                roughenedArgs[i] = o;
            }
        }

        for (int i = 0; i < args1.length; i++) {
            if (roughenedArgs[i] == null) {
                roughenedArgs[i] = args1[i];
            }
        }
        return roughenedArgs;
    }

    private static List toList(Object arg) {
        List list = new ArrayList();
        if (arg instanceof int[]) {
            for (int i : (int[]) arg) {
                list.add(i);
            }
        } else if (arg instanceof long[]) {
            for (long i : (long[]) arg) {
                list.add(i);
            }
        } else if (arg instanceof boolean[]) {
            for (boolean i : (boolean[]) arg) {
                list.add(i);
            }
        } else if (arg instanceof char[]) {
            for (char i : (char[]) arg) {
                list.add(i);
            }
        } else if (arg instanceof short[]) {
            for (short i : (short[]) arg) {
                list.add(i);
            }
        } else if (arg instanceof byte[]) {
            for (byte i : (byte[]) arg) {
                list.add(i);
            }
        } else if (arg instanceof double[]) {
            for (double i : (double[]) arg) {
                list.add(i);
            }
        } else if (arg instanceof float[]) {
            for (float i : (float[]) arg) {
                list.add(i);
            }
        } else {
            for (Object i : (Object[]) arg) {
                list.add(i);
            }
        }
        return list;
    }

}
