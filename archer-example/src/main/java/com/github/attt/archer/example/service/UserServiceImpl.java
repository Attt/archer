package com.github.attt.archer.example.service;

import com.github.attt.archer.annotation.Cacheable;
import com.github.attt.archer.example.model.User;
import com.github.attt.archer.example.repository.UserRepository;
import com.github.attt.archer.invocation.CacheContext;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * User service
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
@Cacheable(prefix = "archer:example:user")
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl() {
        this.userRepository = new UserRepository();
    }


    @Override
    public List<User> getUsers(int pageId, int pageSize) {
        return userRepository.queryUsers((pageId - 1), pageSize);
    }

    @Override
    public List<User> getUsersWithSpecifiedArea(int pageId, int pageSize) {
        return userRepository.queryUsers((pageId - 1), pageSize);
    }


    @Override
    public List<User> getUsers(List<Long> userIds) {
        return userRepository.queryUsers(userIds);
    }

    @Override
    public List<User> getUsersOrderByAgeReversed(List<Long> userIds) {
        List<User> userList = userRepository.queryUsers(userIds);
        userList.sort(Comparator.comparingInt(User::getAge).reversed());
        return userList;
    }


    @Override
    public List<User> getUsersWithMultipleParameters(String flag, List<Long> userIds) {
        List<User> userList = userRepository.queryUsers(userIds);
        userList.sort(Comparator.comparingInt(User::getAge).reversed());
        return userList;
    }


    @Override
    public User getUser(Long userId) {
        return userRepository.queryUser(userId);
    }


    @Override
    public void addUser(User user) {
        userRepository.addUser(user);
    }

    @Override
    public void deleteUsers(List<Long> userIds) {
        for (Long userId : userIds) {
            userRepository.deleteUser(userId);
        }
    }

    @Override
    public void deleteAllUser() {
        // disabled for retest
//        userRepository.deleteAllUser();
    }

    @Override
    public void deleteAllCustomAreaUser() {
        // disabled for retest
//        userRepository.deleteAllUser();
    }

    @Override
    public void renameUser(Long userId, String userName) {
        User user = userRepository.queryUser(userId);
        if (user != null) {
            CacheContext.evictList("archer:example:user:page:1");
            userRepository.renameUser(userId, userName);
        }
    }

    @Override
    public void deleteUser(Long userId) {
        User user = userRepository.queryUser(userId);
        if (user != null) {
            userRepository.deleteUser(userId);
        }
    }
}
