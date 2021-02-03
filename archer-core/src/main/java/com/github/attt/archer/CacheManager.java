package com.github.attt.archer;

import com.github.attt.archer.cache.ShardingCache;
import com.github.attt.archer.cache.KeyGenerator;
import com.github.attt.archer.cache.Serializer;
import com.github.attt.archer.constants.Serialization;
import com.github.attt.archer.exception.CacheOperationException;
import com.github.attt.archer.expression.CacheExpressionUtilObject;
import com.github.attt.archer.annotation.metadata.CacheMetadata;
import com.github.attt.archer.annotation.config.AbstractCacheProperties;
import com.github.attt.archer.annotation.config.EvictionProperties;
import com.github.attt.archer.invoker.ListCacheInvoker;
import com.github.attt.archer.invoker.CacheInvoker;
import com.github.attt.archer.invoker.AbstractInvoker;
import com.github.attt.archer.roots.Component;
import com.github.attt.archer.roots.ListComponent;
import com.github.attt.archer.roots.ObjectComponent;
import com.github.attt.archer.metrics.api.listener.CacheMetricsListener;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.SpringElUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache manager
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@SuppressWarnings("all")
public class CacheManager implements Component {

    public static final String INTERNAL_CACHE_MANAGER_BEAN_NAME = "archar.cache.internalCacheManager";

    /**
     * cache config properties
     */
    public static class Config {
        public static boolean metricsEnabled = false;
        public static Serialization valueSerialization = Serialization.JAVA;
    }

    /**
     * Sharding cache
     */
    private ShardingCache shardingCache;

    /**
     * Manage all key generators
     */
    private Map<String, KeyGenerator> keyGeneratorMap = new ConcurrentHashMap<>();

    /**
     * Manage all serializers
     */
    private Map<String, Serializer> serializerMap = new ConcurrentHashMap<>();

    /**
     * Manage all cache cache properties
     */
    private Map<String, AbstractCacheProperties> cachePropertiesMap = new ConcurrentHashMap<>();


    /**
     * Manage all cache eviction cache properties
     */
    private Map<String, EvictionProperties> evictionPropertiesMap = new ConcurrentHashMap<>();

    /**
     * Manage all method mapping to cache properties name
     */
    private Map<String, List<String>> methodSignatureToPropertiesName = new ConcurrentHashMap<>();

    /**
     * Manage all processors
     */
    private Map<String, AbstractInvoker> invokersMap = new ConcurrentHashMap<>();

    /**
     * Manage all cache stats listeners
     */
    private Map<String, CacheMetricsListener> statsListenerMap = new ConcurrentHashMap<>();


    public ShardingCache getShardingCache() {
        return shardingCache;
    }

    public void setShardingCache(ShardingCache shardingCache) {
        this.shardingCache = shardingCache;
    }

    public Map<String, AbstractCacheProperties> getCachePropertiesMap() {
        return cachePropertiesMap;
    }

    public void setCachePropertiesMap(Map<String, AbstractCacheProperties> cachePropertiesMap) {
        this.cachePropertiesMap = cachePropertiesMap;
    }

    public Map<String, EvictionProperties> getEvictionPropertiesMap() {
        return evictionPropertiesMap;
    }

    public void setEvictionPropertiesMap(Map<String, EvictionProperties> evictionPropertiesMap) {
        this.evictionPropertiesMap = evictionPropertiesMap;
    }

    public Map<String, List<String>> getMethodSignatureToPropertiesName() {
        return methodSignatureToPropertiesName;
    }

    public void setMethodSignatureToPropertiesName(Map<String, List<String>> methodSignatureToPropertiesName) {
        this.methodSignatureToPropertiesName = methodSignatureToPropertiesName;
    }


    public Map<String, KeyGenerator> getKeyGeneratorMap() {
        return keyGeneratorMap;
    }

    public void setKeyGeneratorMap(Map<String, KeyGenerator> keyGeneratorMap) {
        this.keyGeneratorMap = keyGeneratorMap;
    }

    public Map<String, Serializer> getSerializerMap() {
        return serializerMap;
    }

    public void setSerializerMap(Map<String, Serializer> serializerMap) {
        this.serializerMap = serializerMap;
    }

    public Map<String, CacheMetricsListener> getStatsListenerMap() {
        return statsListenerMap;
    }

    public void setStatsListenerMap(Map<String, CacheMetricsListener> statsListenerMap) {
        this.statsListenerMap = statsListenerMap;
    }

    /**
     * Get cache properties by method signature and properties type
     *
     * @param methodSignature
     * @param propertiesType
     * @param <T>
     * @return
     */
    public <T extends AbstractCacheProperties> List<T> getCacheProperties(String methodSignature, Class<T> propertiesType) {
        List<String> configNames = methodSignatureToPropertiesName.getOrDefault(methodSignature, null);

        List<T> sources = new ArrayList<>();
        if (!CommonUtils.isEmpty(configNames)) {
            for (String configName : configNames) {
                AbstractCacheProperties<CacheMetadata, Object> cacheProperties = cachePropertiesMap.getOrDefault(configName, null);
                if (cacheProperties == null || !propertiesType.isAssignableFrom(cacheProperties.getClass())) {
                    continue;
                }
                sources.add((T) cacheProperties);
            }

        }
        return sources;
    }

    /**
     * Get eviction properties by method signature
     *
     * @param methodSignature
     * @return
     */
    public List<EvictionProperties> getEvictionProperties(String methodSignature) {
        List<String> configNames = methodSignatureToPropertiesName.getOrDefault(methodSignature, null);

        List<EvictionProperties> evictionConfigs = new ArrayList<>();
        if (!CommonUtils.isEmpty(configNames)) {
            for (String configName : configNames) {
                EvictionProperties evictionProperties = evictionPropertiesMap.getOrDefault(configName, null);
                if (evictionProperties == null) {
                    continue;
                }
                evictionConfigs.add(evictionProperties);
            }

        }
        return evictionConfigs;
    }

    /**
     * Get all processors
     *
     * @return
     */
    public Collection<AbstractInvoker> getInvokers(){
        return invokersMap.values();
    }

    /**
     * Get cache invoker which matches cache properties
     *
     * @param cacheProperties
     * @return
     */
    public AbstractInvoker getProcessor(AbstractCacheProperties cacheProperties) {
        List<String> componentInterfaces = new ArrayList<>();
        Class<?>[] interfaces = cacheProperties.getClass().getInterfaces();
        for (Class<?> ifc : interfaces) {
            if (Component.class.isAssignableFrom(ifc)) {
                componentInterfaces.add(ifc.toString());
            }
        }
        for (String componentInterface : componentInterfaces) {
            if (invokersMap.containsKey(componentInterface)) {
                return invokersMap.get(componentInterface);
            }
        }


        throw new CacheOperationException("Illegal cache properties provided. " + cacheProperties.getClass().getName());
    }

    /**
     * Get cache processor which matches component type
     *
     * @param type
     * @param <C>
     * @return
     */
    public <C extends Component> AbstractInvoker getProcessor(Class<C> type) {
        return invokersMap.getOrDefault(type.toString(), null);
    }

    @Override
    public void initialized() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("util", CacheExpressionUtilObject.class);
        SpringElUtil.init(vars);

        // note: add complex cache processor before object cache processor
        // because complex cache may has the same key name with object cache
        // but need deal with more data than object cache when evicting

        ListCacheInvoker<?> listServiceCacheProcessor = new ListCacheInvoker<>();
        listServiceCacheProcessor.afterInitialized(this);
        invokersMap.put(ListComponent.class.toString(), listServiceCacheProcessor);

        CacheInvoker<?> objectServiceCacheProcessor = new CacheInvoker<>();
        objectServiceCacheProcessor.afterInitialized(this);
        invokersMap.put(ObjectComponent.class.toString(), objectServiceCacheProcessor);
    }

    @Override
    public String initializedInfo() {
        return "\r\n" + "cache properties : " + cachePropertiesMap.size() + "\r\n"
                + "eviction properties : " + evictionPropertiesMap.size() + "\r\n"
                + "processors : \r\n" + invokersMap + "\r\n";
    }
}
