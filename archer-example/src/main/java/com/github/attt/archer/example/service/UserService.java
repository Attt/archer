package com.github.attt.archer.example.service;


import com.github.attt.archer.annotation.Cache;
import com.github.attt.archer.annotation.CacheEvict;
import com.github.attt.archer.annotation.CacheList;
import com.github.attt.archer.annotation.CacheMapping;
import com.github.attt.archer.example.model.User;

import java.util.List;

import static com.github.attt.archer.constants.Constants.DEFAULT_REGION;

/**
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface UserService {

    // 列表缓存[

    /**
     * 带条件的列表缓存
     * <p>
     * <p>
     * condition 用来限制缓存条件
     *
     * @param pageId
     * @param pageSize
     * @return
     */
    @CacheList(key = "'page:' + #pageId", element = @Cache(key = "#result$each.id"), condition = "#pageId == 1")
    List<User> getUsers(int pageId, int pageSize);

    /**
     * 带条件的列表缓存, 指定area
     * <p>
     * <p>
     * condition 用来限制缓存条件
     *
     * @param pageId
     * @param pageSize
     * @return
     */
    @CacheList(region = "custom", element = @Cache(region = "custom", key = "#result$each.id"), key = "'page:' + #pageId", condition = "#pageId == 1")
    List<User> getUsersWithSpecifiedArea(int pageId, int pageSize);

    // 对象缓存

    /**
     * 对象缓存
     *
     * @param userId
     * @return
     */
    @Cache(key = "#userId")
    User getUser(Long userId);

    /**
     * 多对象缓存
     * <p>
     * 返回结果和参数顺序一致
     *
     * @param userIds
     * @return
     */
    @Cache(multi = true, key = "#userIds$each")
    List<User> getUsers(@CacheMapping("id") List<Long> userIds);

    /**
     * 多对象缓存
     * <p>
     * 参数顺序与结果不一致
     *
     * @param userIds
     * @return
     */
    @Cache(multi = true, key = "#userIds$each")
    List<User> getUsersOrderByAgeReversed(@CacheMapping("id") List<Long> userIds);

    /**
     * 多对象缓存
     * <p>
     * 多参数
     *
     * @param flag
     * @param userIds
     * @return
     */
    @Cache(multi = true, key = "#userIds$each")
    List<User> getUsersWithMultipleParameters(String flag, @CacheMapping("id") List<Long> userIds);

    // 淘汰缓存

    /**
     * 淘汰缓存
     *
     * @param user
     */
    @CacheEvict(keys = "#user.id")
    void addUser(User user);


    /**
     * 淘汰缓存
     *
     * @param userIds
     */
    @CacheEvict(multi = true, keys = "#userIds$each")
    void deleteUsers(List<Long> userIds);

    /**
     * Deprecated since 1.0-rc4
     * due to regions is considered now even if all is set to true
     * <p>
     * 淘汰整个缓存区域
     */
    @Deprecated
    @CacheEvict(all = true)
    void deleteAllUser();

    /**
     * 淘汰整个缓存区域, 指定area
     */
    @CacheEvict(all = true, regions = "custom")
    void deleteAllCustomAreaUser();

    /**
     * 淘汰整个缓存区域, 所有area
     */
    @CacheEvict(all = true, regions = {"custom", DEFAULT_REGION})
    void deleteAllAreaUser();

    /**
     * 淘汰缓存
     * <p>
     * 使用CacheContext手动淘汰缓存
     *
     * @param userId
     * @param userName
     */
    void renameUser(Long userId, String userName);

    /**
     * 淘汰缓存
     * <p>
     * 使用CacheContext手动淘汰缓存
     *
     * @param userId
     */
    void deleteUser(Long userId);

}
