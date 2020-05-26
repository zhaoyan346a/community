package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 点赞
     *
     * @param userId       发起点赞的用户id
     * @param entityType   被点赞的实体类型  1表示帖子 2表示评论
     * @param entityId     被点赞的实体id
     * @param entityUserId 被点赞的实体对应的用户id
     */
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        //在execute里面执行事务
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //获取点赞评论的key
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                //获取用户被点赞的key
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                //判断是否已经点赞过
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);
                //开启事务
                operations.multi();
                if (isMember) {//已经点赞过
                    operations.opsForSet().remove(entityLikeKey, userId);//取消点赞
                    operations.opsForValue().decrement(userLikeKey);//用户被赞数减1
                } else {//没点赞过
                    operations.opsForSet().add(entityLikeKey, userId);//点赞
                    operations.opsForValue().increment(userLikeKey);//用户被赞数加1
                }
                //执行事务的命令队列中的命令
                return operations.exec();
            }
        });
    }

    // 查询某实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        //获取点赞评论的key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        //返回点赞数量，也就是set里面userId的数量
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        //获取点赞评论的key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        //1：点赞了 0：没点赞 -1：点踩
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    // 查询某个用户获得的赞的数量
    public int findUserLikeCount(int userId) {
        //获取用户被赞的key
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }

}
