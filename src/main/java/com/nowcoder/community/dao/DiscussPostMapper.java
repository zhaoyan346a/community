package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    // userId为0时：查找所有讨论帖
    // offset limit 相当于 limit a , b
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    // @Param注解用于给参数取别名,
    // 如果只有一个参数,并且在<if>里使用,则必须加别名.
    // 查询讨论帖的个数
    int selectDiscussPostRows(@Param("userId") int userId);

    //插入讨论帖
    int insertDiscussPost(DiscussPost discussPost);

    //查询讨论帖
    DiscussPost selectDiscussPostById(int id);

    //更新帖子的评论数
    int updateCommentCount(int id, int commentCount);

    //更新帖子类别  0-普通 1-置顶
    int updateType(int id, int type);

    //更新帖子状态  0-正常; 1-精华; 2-拉黑
    int updateStatus(int id, int status);
}
