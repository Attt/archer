package com.github.attt.archer.example.service;


import com.github.attt.archer.annotation.Cache;
import com.github.attt.archer.annotation.CacheList;
import com.github.attt.archer.annotation.CacheEvict;
import com.github.attt.archer.cache.annotation.*;
import com.github.attt.archer.cache.annotation.extra.MapTo;
import com.github.attt.archer.example.model.User;

import java.util.List;

import static com.github.attt.archer.constants.Constants.DEFAULT_AREA;

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
    @CacheList(key = "'page:' + #pageId", elementKey = "#result$each.id", condition = "#pageId == 1")
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
    @CacheList(area = "custom", elementArea = "custom", key = "'page:' + #pageId", elementKey = "#result$each.id", condition = "#pageId == 1")
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
    @CacheMulti(elementKey = "#userIds$each")
    List<User> getUsers(@MapTo("#result$each.id") List<Long> userIds);

    /**
     * 多对象缓存
     * <p>
     * 参数顺序与结果不一致
     *
     * @param userIds
     * @return
     */
    @CacheMulti(elementKey = "#userIds$each", orderBy = "#result$each.userOrder()")
    List<User> getUsersOrderByAgeReversed(@MapTo("#result$each.id") List<Long> userIds);

    /**
     * 多对象缓存
     * <p>
     * 多参数
     *
     * @param flag
     * @param userIds
     * @return
     */
    @CacheMulti(elementKey = "#userIds$each")
    List<User> getUsersWithMultipleParameters(String flag, @MapTo("#result$each.id") List<Long> userIds);

    // 淘汰缓存

    /**
     * 淘汰缓存
     *
     * @param user
     */
    @CacheEvict(key = "#user.id")
    void addUser(User user);


    /**
     * 淘汰缓存
     *
     * @param userIds
     */
    @EvictMulti(elementKey = "#userIds.size()")
    void deleteUsers(List<Long> userIds);

    /**
     * 淘汰整个缓存区域
     */
    @CacheEvict(all = true)
    void deleteAllUser();

    /**
     * 淘汰整个缓存区域, 指定area
     */
    @CacheEvict(all = true, area = "custom")
    void deleteAllCustomAreaUser();

    /**
     * 淘汰整个缓存区域, 所有area
     */
    @CacheEvict(all = true, area = {"custom", DEFAULT_AREA})
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
