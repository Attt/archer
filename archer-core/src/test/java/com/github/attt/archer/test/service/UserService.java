package com.github.attt.archer.test.service;

import com.github.attt.archer.annotation.Cache;
import com.github.attt.archer.annotation.CacheList;
import com.github.attt.archer.annotation.CacheMapping;
import com.github.attt.archer.test.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author atpex
 * @since 1.0
 */
public class UserService {

    @Cache(key = "'user:' + #userId")
    public User getUserById(long userId) {
        User user = new User();
        user.setAge(30);
        user.setId(userId);
        user.setUser("atpex");
        return user;
    }

    @Cache(key = "'user:' + #userIds$each", multi = false)
    public List<User> getUsersByIdList(@CacheMapping(toResult = "id") List<Long> userIds) {
        List<User> users = new ArrayList<>();
        for (Long userId : userIds) {
            users.add(getUserById(userId));
        }
        return users;
    }

    @Cache(key = "'user:list'")
    public List<User> getUsersByIdListAsOne(List<Long> userIds) {
        List<User> users = new ArrayList<>();
        for (Long userId : userIds) {
            users.add(getUserById(userId));
        }
        return users;
    }


    @CacheList(key = "'user:page:' + #pageId", element = @Cache(key = "'user:' + #result$each.id"))
    public List<User> getPagingUsers(int pageId, int pageSize) {
        List<User> users = new ArrayList<>();
        for (Long userId : Arrays.asList(1L, 2L)) {
            users.add(getUserById(userId));
        }
        return users;
    }

}
