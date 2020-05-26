package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {
    /*
    查询评论内容
    重点注意：假设没有entityType
        因为同一个用户(userId)在一个帖子下面评论了，也在评论下面回复了，这时候如果帖子的
        entityId=1并且 评论的entityId=1，这时就不知道该用户的entityId是帖子的还是评论的了；
        所以要用entityType+entityId唯一的确定该条评论(comment)是“回复给帖子的”还是“回复给评论的”。
        解决了帖子ID与评论ID相同时不能分辨出回复给帖子还是评论的情况。

        说的再简单点：
        当entityType== ENTITY_TYPE_POST(1)时，entityId是discuss_post里面的id,也就是帖子ID
        当entityType== ENTITY_TYPE_COMMENT(2)时，entityId是comment里面的id,也就是评论ID
     */
    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    //查询评论数
    int selectCountByEntity(int entityType, int entityId);

    //插入评论
    int insertComment(Comment comment);
}
