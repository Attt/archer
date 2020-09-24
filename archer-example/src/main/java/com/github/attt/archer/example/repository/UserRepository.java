package com.github.attt.archer.example.repository;

import com.github.attt.archer.example.model.User;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class UserRepository {

    public static final int TOTAL = 1000;

    public static final NavigableMap<Long, User> USER_TABLE = Collections.synchronizedNavigableMap(new TreeMap<>());

    public static final NavigableMap<String, List<User>> USER_TABLE_NAME_KEY = Collections.synchronizedNavigableMap(new TreeMap<>());

    public UserRepository() {
        // init user data
        for (int i = 0; i < TOTAL; i++) {
            String name = "exampleUser" + (i % 20);
            User user = new User();
            user.setId((long) i);
            user.setName(name);
            user.setAge(i % 100);
            USER_TABLE.put((long) i, user);
            USER_TABLE_NAME_KEY.computeIfAbsent(name, s -> new ArrayList<>()).add(user);
        }
    }

    public List<User> queryUsers(int offset, int count) {
        lag();
        Collection<User> values = USER_TABLE.subMap((long) (offset * count), (long) ((offset + 1) * count)).values();
        return new ArrayList<>(values);
    }

    public User queryUser(long userId) {
        lag();
        return USER_TABLE.get(userId);
    }

    public List<User> queryUsers(List<Long> userIds) {
        lag();
        List<User> userList = new ArrayList<>();
        for (Long userId : userIds) {
            userList.add(USER_TABLE.get(userId));
        }
        return userList;
    }

    public List<User> queryUsers(String userName) {
        lag();
        return USER_TABLE_NAME_KEY.get(userName);
    }

    public void addUser(User user) {
        lag();
        if (user != null) {
            if (user.getId() != null && !StringUtils.isEmpty(user.getName())) {
                USER_TABLE.put(user.getId(), user);
                USER_TABLE_NAME_KEY.computeIfAbsent(user.getName(), s -> new ArrayList<>()).add(user);
            }
        }
    }

    public void deleteUser(String userName) {
        lag();
        List<User> userList = USER_TABLE_NAME_KEY.get(userName);
        if (userList != null) {
            USER_TABLE_NAME_KEY.remove(userName);
            for (User user : userList) {
                USER_TABLE.remove(user.getId());
            }
        }
    }

    public void deleteUser(long userId) {
        lag();
        User user = USER_TABLE.get(userId);
        if (user != null) {
            USER_TABLE.remove(userId);
            List<User> userList = USER_TABLE_NAME_KEY.get(user.getName());
            List<User> usersRemain = new ArrayList<>();
            for (User oldUser : userList) {
                if (userId != oldUser.getId()) {
                    usersRemain.add(oldUser);
                }
            }
            USER_TABLE_NAME_KEY.put(user.getName(), usersRemain);
        }
    }

    public void deleteAllUser() {
        USER_TABLE.clear();
        USER_TABLE_NAME_KEY.clear();
    }

    public void renameUser(long userId, String username) {
        lag();
        User user = USER_TABLE.get(userId);
        if (user != null) {
            String oldName = user.getName();
            List<User> userList = USER_TABLE_NAME_KEY.get(oldName);
            List<User> usersRemain = new ArrayList<>();
            for (User oldUser : userList) {
                if (userId != oldUser.getId()) {
                    usersRemain.add(oldUser);
                }
            }
            USER_TABLE_NAME_KEY.put(oldName, usersRemain);

            user.setName(username);
            USER_TABLE_NAME_KEY.computeIfAbsent(username, s -> new ArrayList<>()).add(user);
        }
    }

    private void lag() {
//        try {
//            TimeUnit.SECONDS.sleep(1);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

}
