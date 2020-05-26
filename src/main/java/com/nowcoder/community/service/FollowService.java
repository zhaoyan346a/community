package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class FollowService {

    @Autowired
    private RedisTemplate redisTemplate;

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

}
