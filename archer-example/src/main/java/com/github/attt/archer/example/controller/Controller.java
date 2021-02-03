package com.github.attt.archer.example.controller;


import com.github.attt.archer.example.model.User;
import com.github.attt.archer.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author atpexgo.wu
 * @since 1.0.0
 */
@RestController
public class Controller {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/getPaginatedUsers", method = RequestMethod.GET)
    List<User> getUsers(int pageId, int pageSize) {
        return userService.getUsers(pageId, pageSize);
    }

    @RequestMapping(value = "/getUsersWithSpecifiedArea", method = RequestMethod.GET)
    List<User> getUsersWithSpecifiedArea(int pageId, int pageSize) {
        return userService.getUsersWithSpecifiedArea(pageId, pageSize);
    }

    @RequestMapping(value = "/getUsers", method = RequestMethod.GET)
    List<User> getUsers() {
        List<Long> userIds = new ArrayList<>();
        userIds.add(8L);
        userIds.add(107L);
        userIds.add(203L);
        userIds.add(404L);
        userIds.add(505L);
        return userService.getUsers(userIds);
    }

    /**
     * Remove order support since version 1.0-rc4
     * @return
     */
    @Deprecated
    @RequestMapping(value = "/getUsersOrderByAgeReversed", method = RequestMethod.GET)
    List<User> getUsersOrderByAgeReversed() {
        List<Long> userIds = new ArrayList<>();
        userIds.add(8L);
        userIds.add(107L);
        userIds.add(203L);
        userIds.add(404L);
        userIds.add(505L);
        return userService.getUsersOrderByAgeReversed(userIds);
    }

    @RequestMapping(value = "/getUsersWithMultipleParameters", method = RequestMethod.GET)
    List<User> getUsersWithMultipleParameters() {
        List<Long> userIds = new ArrayList<>();
        userIds.add(8L);
        userIds.add(107L);
        userIds.add(203L);
        userIds.add(404L);
        userIds.add(505L);
        return userService.getUsersWithMultipleParameters("testest", userIds);
    }

    @RequestMapping(value = "/deleteUsers", method = RequestMethod.GET)
    void deleteUser() {
        List<Long> userIds = new ArrayList<>();
        userIds.add(8L);
        userIds.add(107L);
        userIds.add(203L);
        userIds.add(404L);
        userIds.add(505L);
        userService.deleteUsers(userIds);
    }

    @RequestMapping(value = "/getUser", method = RequestMethod.GET)
    User getUser(Long userId) {
        return userService.getUser(userId);
    }

    @RequestMapping(value = "/addUser", method = RequestMethod.POST)
    void addUser(@RequestBody User user) {
        userService.addUser(user);
    }

    @RequestMapping(value = "/renameUser", method = RequestMethod.POST)
    void renameUser(Long userId, String userName) {
        userService.renameUser(userId, userName);
    }

    @RequestMapping(value = "/deleteUser", method = RequestMethod.POST)
    void deleteUser(Long userId) {
        userService.deleteUser(userId);
    }

    @RequestMapping(value = "/deleteAllUser", method = RequestMethod.POST)
    void deleteAllUser() {
        userService.deleteAllUser();
    }

    @RequestMapping(value = "/deleteAllCustomAreaUser", method = RequestMethod.POST)
    void deleteAllCustomAreaUser() {
        userService.deleteAllCustomAreaUser();
    }

    @RequestMapping(value = "/deleteAllAreaUser", method = RequestMethod.POST)
    void deleteAllAreaUser() {
        userService.deleteAllAreaUser();
    }
}
