package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞
    public void like(int userId, int entityType, int entityId) {
        //获取点赞评论的key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        //判断是否已经点赞过
        boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
        if (isMember) {//已经点赞过
            redisTemplate.opsForSet().remove(entityLikeKey, userId);
        } else {//没点赞过
            redisTemplate.opsForSet().add(entityLikeKey, userId);
        }
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

}
