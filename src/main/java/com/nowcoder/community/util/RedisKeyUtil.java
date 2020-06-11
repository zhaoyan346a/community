package com.nowcoder.community.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
点赞：set
关注：sorted set

 */
public class RedisKeyUtil {

    private static final String SPLIT = ":";//分隔符
    private static final String PREFIX_ENTITY_LIKE = "like:entity";//帖子、评论、回复等点赞实体的前缀
    private static final String PREFIX_USER_LIKE = "like:user";//用户被赞的前缀
    private static final String PREFIX_FOLLOW = "follow";//正在关注实体的前缀
    private static final String PREFIX_FOLLOWER = "follower";//粉丝的前缀
    private static final String PREFIX_KAPTCHA = "kaptcha";//验证码的前缀
    private static final String PREFIX_TICKET = "ticket";//登录凭证的前缀
    private static final String PREFIX_USER = "user";//登录用户的前缀
    private static final String PREFIX_UV = "uv";//unique visit的前缀(ip)
    private static final String PREFIX_DAU = "dau";//daily active user(日活跃用户)的前缀(userId)
    private static final String PREFIX_POST = "post";//帖子前缀(计算分数)

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
    某个实体拥有的粉丝
    userId表示用户的id；now表示点了关注的时间
    follower:entityType:entityId -> zset(userId,now)
     */
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    /*
    验证码对应的key  owner表示验证码的唯一标识
    kaptcha:sadasdajkfhjfk(唯一标识)
     */
    public static String getKaptchaKey(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    /*
    登录的凭证对应的key
    ticket:登陆凭证字符串
     */
    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }

    /*
    登录的用户对应的key
    user:userId
     */
    public static String getUserKey(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }

    /*
    单日UV对应的key  统计IP
    uv:20200101 yyyyMMdd
     */
    public static String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    /*
    区间日期UV对应的key  统计IP
    uv:20200101:20200202 yyyyMMdd
     */
    public static String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    /*
    单日DAU对应的key  统计userId个数
    dau:20200101  yyyyMMdd
     */
    public static String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    /*
    区间日期DAU对应的key  统计userId个数
    dau:20200101:20200202 yyyyMMdd
     */
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    /*
    计算帖子分数的key  set(帖子ID,...)
    post:score
    发帖，加精，给帖子评论，给帖子点赞 都会把postId添加到set里，定时去计算帖子分数
     */
    public static String getPostScoreKey() {
        return PREFIX_POST + SPLIT + "score";
    }
}
