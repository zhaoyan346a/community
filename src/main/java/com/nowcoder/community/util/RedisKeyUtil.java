package com.nowcoder.community.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":";//分隔符
    private static final String PREFIX_ENTITY_LIKE = "like:entity";//点赞实体前缀

    // 某个实体的赞
    // redis中key为(like:entity:entityType:entityId)的set集合（set里存放的是userId） -> set(userId)
    // 唯一确定一条评论（给帖子的/给评论的）
    /*
    因为帖子id与评论id是可能会重复的,所以 entityType + entityId能唯一确定帖子点赞数和评论点赞数。
     */
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

}
