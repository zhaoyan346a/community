package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;

    /**
     * 关注
     *
     * @param userId     用户id
     * @param entityType 实体类型
     * @param entityId   实体id
     */
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //获取该用户正在关注某实体类型 对应的key
                String followKey = RedisKeyUtil.getFollowKey(userId, entityType);
                //获取该实体的粉丝 对应的key
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                //开启事务
                operations.multi();
                //往zset里添加 正在关注里添加实体id； 粉丝里添加用户id
                operations.opsForZSet().add(followKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                //执行命令
                return operations.exec();
            }
        });
    }

    /**
     * 取关
     *
     * @param userId     用户id
     * @param entityType 实体类型
     * @param entityId   实体id
     */
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //获取该用户正在关注某实体类型 对应的key
                String followKey = RedisKeyUtil.getFollowKey(userId, entityType);
                //获取该实体的粉丝 对应的key
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                //开启事务
                operations.multi();
                //在zset里删除 正在关注里删除实体id； 粉丝里删除用户id
                operations.opsForZSet().remove(followKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);
                //执行命令
                return operations.exec();
            }
        });
    }

    // 查询关注的实体的数量
    public long findFollowCount(int userId, int entityType) {
        //获取正在关注某实体类型 对应的key
        String followKey = RedisKeyUtil.getFollowKey(userId, entityType);
        //zCard 查询zset中的元素个数
        return redisTemplate.opsForZSet().zCard(followKey);
    }

    // 查询实体的粉丝的数量
    public long findFollowerCount(int entityType, int entityId) {
        // 获取某实体类型 对应的key
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        //zCard 查询zset中的元素个数
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    // 查询当前用户是否已关注该实体
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        //获取正在关注某实体类型 对应的key
        String followKey = RedisKeyUtil.getFollowKey(userId, entityType);
        //如果zset中没有entityId对应的分数，那么就是没关注该实体，返回false
        return redisTemplate.opsForZSet().score(followKey, entityId) != null;
    }

    // 查询某用户关注的人
    public List<Map<String, Object>> findFollowes(int userId, int offset, int limit) {
        return core(userId, offset, limit, true);
    }

    // 查询某用户的粉丝
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        return core(userId, offset, limit, false);
    }

    /**
     * 查询正在关注/粉丝列表的方法
     *
     * @param userId
     * @param offset
     * @param limit
     * @param follow true:查询正在关注；false:查询粉丝列表
     * @return
     */
    private List<Map<String, Object>> core(int userId, int offset, int limit, boolean follow) {
        String key = "";
        if (follow)
            //正在关注对应的key
            key = RedisKeyUtil.getFollowKey(userId, ENTITY_TYPE_USER);
        else
            //粉丝列表对应的key
            key = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        //虽然java里的Set是无序集合，但是redis的具体Set接口的实现类是有序集合的实现
        // 没有 withscores，只返回了key，没有score
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(
                key, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            //添加用户
            map.put("user", user);
            //获取userId对应的分数
            Double score = redisTemplate.opsForZSet().score(key, targetId);
            //转换成日期
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }
}
