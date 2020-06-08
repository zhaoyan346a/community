package com.nowcoder.community.service;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    //查询讨论帖
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit) {
        return discussPostMapper.selectDiscussPosts(userId, offset, limit);
    }

    //统计某个用户的讨论帖数量
    public int findDiscussPostRows(int userId) {
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    // 插入DiscussPost
    public int addDiscussPost(DiscussPost post) {
        if (post == null)
            throw new IllegalArgumentException("参数不能为空!");
        // 转义html标记  org.springframework.web.util.HtmlUtils.htmlEscape 可以把 < 转成 &lt;
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    // 查询特定id的讨论帖
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    //更新帖子评论数
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    //更新帖子类别  0-普通 1-置顶
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    //更新帖子状态  0-正常; 1-精华; 2-拉黑
    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }
}
