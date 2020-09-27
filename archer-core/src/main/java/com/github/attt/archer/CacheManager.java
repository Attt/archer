package com.github.attt.archer;

import com.github.attt.archer.cache.internal.ShardingCache;
import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.components.api.Serializer;
import com.github.attt.archer.constants.Serialization;
import com.github.attt.archer.exception.CacheOperationException;
import com.github.attt.archer.expression.CacheExpressionUtilObject;
import com.github.attt.archer.metadata.CacheMetadata;
import com.github.attt.archer.operation.api.AbstractCacheOperation;
import com.github.attt.archer.operation.EvictionOperation;
import com.github.attt.archer.processor.ListProcessor;
import com.github.attt.archer.processor.ObjectProcessor;
import com.github.attt.archer.processor.api.AbstractProcessor;
import com.github.attt.archer.roots.Component;
import com.github.attt.archer.roots.ListComponent;
import com.github.attt.archer.roots.ObjectComponent;
import com.github.attt.archer.stats.api.listener.CacheStatsListener;
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
     * Sharding cache operation
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
     * Manage all cache acceptation operation sources
     */
    private Map<String, AbstractCacheOperation> cacheOperationMap = new ConcurrentHashMap<>();


    /**
     * Manage all cache eviction operation sources
     */
    private Map<String, EvictionOperation> evictionOperationMap = new ConcurrentHashMap<>();

    /**
     * Manage all method mapping to operation source name
     */
    private Map<String, List<String>> methodSignatureToOperationSourceName = new ConcurrentHashMap<>();

    /**
     * Manage all processors
     */
    private Map<String, AbstractProcessor> processors = new ConcurrentHashMap<>();

    /**
     * Manage all cache stats listeners
     */
    private Map<String, CacheStatsListener> statsListenerMap = new ConcurrentHashMap<>();


    public ShardingCache getShardingCache() {
        return shardingCache;
    }

    public void setShardingCache(ShardingCache shardingCache) {
        this.shardingCache = shardingCache;
    }

    public Map<String, AbstractCacheOperation> getCacheOperationMap() {
        return cacheOperationMap;
    }

    public void setCacheOperationMap(Map<String, AbstractCacheOperation> cacheOperationMap) {
        this.cacheOperationMap = cacheOperationMap;
    }

    public Map<String, EvictionOperation> getEvictionOperationMap() {
        return evictionOperationMap;
    }

    public void setEvictionOperationMap(Map<String, EvictionOperation> evictionOperationMap) {
        this.evictionOperationMap = evictionOperationMap;
    }

    public Map<String, List<String>> getMethodSignatureToOperationSourceName() {
        return methodSignatureToOperationSourceName;
    }

    public void setMethodSignatureToOperationSourceName(Map<String, List<String>> methodSignatureToOperationSourceName) {
        this.methodSignatureToOperationSourceName = methodSignatureToOperationSourceName;
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

    public Map<String, CacheStatsListener> getStatsListenerMap() {
        return statsListenerMap;
    }

    public void setStatsListenerMap(Map<String, CacheStatsListener> statsListenerMap) {
        this.statsListenerMap = statsListenerMap;
    }

    /**
     * Get acceptation operations by method signature and source type
     *
     * @param methodSignature
     * @param sourceType
     * @param <T>
     * @return
     */
    public <T extends AbstractCacheOperation> List<T> getCacheOperations(String methodSignature, Class<T> sourceType) {
        List<String> configNames = methodSignatureToOperationSourceName.getOrDefault(methodSignature, null);

        List<T> sources = new ArrayList<>();
        if (!CommonUtils.isEmpty(configNames)) {
            for (String configName : configNames) {
                AbstractCacheOperation<CacheMetadata, Object> cacheOperation = cacheOperationMap.getOrDefault(configName, null);
                if (cacheOperation == null || !sourceType.isAssignableFrom(cacheOperation.getClass())) {
                    continue;
                }
                sources.add((T) cacheOperation);
            }

        }
        return sources;
    }

    /**
     * Get eviction operation sources by method signature
     *
     * @param methodSignature
     * @return
     */
    public List<EvictionOperation> getEvictionOperations(String methodSignature) {
        List<String> configNames = methodSignatureToOperationSourceName.getOrDefault(methodSignature, null);

        List<EvictionOperation> evictionConfigs = new ArrayList<>();
        if (!CommonUtils.isEmpty(configNames)) {
            for (String configName : configNames) {
                EvictionOperation evictionOperation = evictionOperationMap.getOrDefault(configName, null);
                if (evictionOperation == null) {
                    continue;
                }
                evictionConfigs.add(evictionOperation);
            }

        }
        return evictionConfigs;
    }

    /**
     * Get all processors
     *
     * @return
     */
    public Collection<AbstractProcessor> getProcessors(){
        return processors.values();
    }

    /**
     * Get cache processor which matches operation source
     *
     * @param cacheOperation
     * @return
     */
    public AbstractProcessor getProcessor(AbstractCacheOperation cacheOperation) {
        List<String> componentInterfaces = new ArrayList<>();
        Class<?>[] interfaces = cacheOperation.getClass().getInterfaces();
        for (Class<?> ifc : interfaces) {
            if (Component.class.isAssignableFrom(ifc)) {
                componentInterfaces.add(ifc.toString());
            }
        }
        for (String componentInterface : componentInterfaces) {
            if (processors.containsKey(componentInterface)) {
                return processors.get(componentInterface);
            }
        }


        throw new CacheOperationException("Illegal cache operation type " + cacheOperation.getClass().getName());
    }

    /**
     * Get cache processor which matches component type
     *
     * @param type
     * @param <C>
     * @return
     */
    public <C extends Component> AbstractProcessor getProcessor(Class<C> type) {
        return processors.getOrDefault(type.toString(), null);
    }

    @Override
    public void initialized() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("util", CacheExpressionUtilObject.class);
        SpringElUtil.init(vars);

        ListProcessor<?> listServiceCacheProcessor = new ListProcessor<>();
        listServiceCacheProcessor.afterInitialized(this);
        processors.put(ListComponent.class.toString(), listServiceCacheProcessor);

        ObjectProcessor<?> objectServiceCacheProcessor = new ObjectProcessor<>();
        objectServiceCacheProcessor.afterInitialized(this);
        processors.put(ObjectComponent.class.toString(), objectServiceCacheProcessor);
    }

    @Override
    public String initializedInfo() {
        return "\r\n" + "cache operations : " + cacheOperationMap.size() + "\r\n"
                + "eviction operations : " + evictionOperationMap.size() + "\r\n"
                + "processors : \r\n" + processors + "\r\n";
    }
}
