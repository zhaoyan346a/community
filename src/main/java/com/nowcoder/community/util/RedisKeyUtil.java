package com.nowcoder.community.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":";//分隔符
    private static final String PREFIX_ENTITY_LIKE = "like:entity";//帖子、评论、回复等点赞实体的前缀
    private static final String PREFIX_USER_LIKE = "like:user";//用户被赞的前缀
    private static final String PREFIX_FOLLOW = "follow";//正在关注实体的前缀
    private static final String PREFIX_FOLLOWER = "follower";//粉丝的前缀

    /*
    某个实体的赞对应的key
    redis中key为(like:entity:entityType:entityId)的set集合（set里存放的是userId） -> set(userId1,userId2...)
    唯一确定一条评论（给帖子的/给评论的）
    因为帖子id与评论id是可能会重复的,所以 entityType + entityId能唯一确定帖子点赞数和评论点赞数。
     */
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    /*
    某个用户的赞对应的key
    like:user:userId -> int
     */
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    /*
    某个用户关注的实体对应的key
    entityType实体包括：用户、帖子、评论、媒体等...
    entityId是实体的id；now表示点了关注的时间
    follow:userId:entityType -> zset(entityId,now)
     */
    public static String getFollowKey(int userId, int entityType) {
        return PREFIX_FOLLOW + SPLIT + userId + SPLIT + entityType;
    }

    /*
    某个实体拥有的粉丝对应的key
    userId表示用户的id；now表示点了关注的时间
    follower:entityType:entityId -> zset(userId,now)
     */
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }
}
