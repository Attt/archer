package com.github.attt.archer.test;

import com.github.attt.archer.Archer;
import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.components.preset.KryoObjectSerializer;
import com.github.attt.archer.constants.Serialization;
import com.github.attt.archer.expression.CacheExpressionUtilObject;
import com.github.attt.archer.test.components.AllCacheEventListener;
import com.github.attt.archer.test.model.User;
import com.github.attt.archer.test.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class ArcherTest {

    @Test
    public void test() {
        UserService userService = Archer.create(ArcherTest.class.getPackage().getName()).init().start(UserService.class);

        User userById = userService.getUserById(1L);

        System.out.println(userById);
    }

    @Test
    public void testWithComponents() {
        Archer.enableMetrics();
        UserService userService = Archer.create(ArcherTest.class.getPackage().getName())
                .addStatsListener(new AllCacheEventListener()).init().start(UserService.class);

        User userById = userService.getUserById(2L);
        System.out.println(userById);

        userById = userService.getUserById(2L);
        System.out.println(userById);
    }

    @Test
    public void testCacheMulti() {
        Archer.enableMetrics();
        Archer.serialization(Serialization.KRYO);
        UserService userService = Archer.create(ArcherTest.class.getPackage().getName())
//                .addKeyGenerator("customKeyGenerator", new CustomKeyGenerator())
//                .addValueSerializer("customValueSerializer", new CustomValueSerializer())
                .addStatsListener(new AllCacheEventListener()).init().start(UserService.class);

        User userById = userService.getUserById(2L);
        System.out.println(userById);

        List<Long> userIds = new ArrayList<>();
        userIds.add(1L);
        userIds.add(2L);
        List<User> usersByIdList = userService.getUsersByIdList(userIds);
        for (User user : usersByIdList) {
            System.out.println(user);
        }

        usersByIdList = userService.getUsersByIdList(userIds);
        for (User user : usersByIdList) {
            System.out.println(user);
        }
    }

    @Test
    public void testUtil() {
        String key = CacheExpressionUtilObject.concatKey(1, "a", "b");
        assert "1:a:b".equals(key);
    }

    @Test
    public void testKryo() {
        KryoObjectSerializer<SimpleClass> valueSerializer = new KryoObjectSerializer<>(SimpleClass.class);
        SimpleClass simpleClass = new SimpleClass("atpex.go", 1);
        byte[] valueBytes = valueSerializer.serialize(simpleClass);

        KryoObjectSerializer<Cache.DefaultEntry> serializer = new KryoObjectSerializer<>(Cache.DefaultEntry.class);
        Cache.DefaultEntry entry = new Cache.DefaultEntry("key1", valueBytes, 123456L);
        byte[] entryBytes = serializer.serialize(entry);

        Cache.DefaultEntry deserializedEntry = serializer.deserialize(entryBytes);

        assert deserializedEntry != null;
        assert deserializedEntry.getKey().equals("key1");
        assert deserializedEntry.getTtl() == 123456L;

        SimpleClass deserializedSimpleClass = valueSerializer.deserialize(deserializedEntry.getValue());

        assert deserializedSimpleClass.name.equals("atpex.go");
        assert deserializedSimpleClass.id == 1;

        KryoObjectSerializer<List<String>> listSerializer = new KryoObjectSerializer<>(List.class);
        List<String> list = new ArrayList<>();
        list.add("attt");
        list.add("apoptoxin4869");
        list.add("apoptoxin4896");
        List<String> nonNoArgsConstructorCollection = Collections.synchronizedList(list);
        byte[] serialized = listSerializer.serialize(nonNoArgsConstructorCollection);
        List<String> deserializedList = listSerializer.deserialize(serialized);

        assert deserializedList != null;
        assert deserializedList.size() == 3;
        for (String l : deserializedList) {
            assert l.equals("attt") || l.equals("apoptoxin4869") || l.equals("apoptoxin4896");
        }
    }

    public static class SimpleClass {
        private String name;
        private Integer id;

        public SimpleClass() {
        }

        public SimpleClass(String name, Integer id) {
            this.name = name;
            this.id = id;
        }
    }

    @Test
    public void testExpiration(){
        long millis = TimeUnit.MILLISECONDS.toMillis(-1L);
        System.out.println(millis);
    }
}
