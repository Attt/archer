package com.github.attt.archer.test;

import com.github.attt.archer.Archer;
import com.github.attt.archer.constants.Serialization;
import com.github.attt.archer.expression.CacheExpressionUtilObject;
import com.github.attt.archer.test.components.AllCacheEventListener;
import com.github.attt.archer.test.model.User;
import com.github.attt.archer.test.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
        Archer.serialization(Serialization.FAST_JSON);
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
    public void testUtil(){
        String key = CacheExpressionUtilObject.concatKey(1, "a", "b");
        assert "1:a:b".equals(key);
    }

}
