package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;

    //添加讨论帖
    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String addDiscussPost(String title, String content) {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "您还没有登录!");
        }

        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        //触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)//发帖主题
                .setUserId(user.getId())//登录用户发的帖子
                .setEntityType(ENTITY_TYPE_POST)//实体类型
                .setEntityId(post.getId());//实体id
        eventProducer.fireEvent(event);

        //报错的情况，将来统一处理
        return CommunityUtil.getJSONString(0, "发布成功!");
    }

    //查询帖子的详情
    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model,
                                 Page page) {
        // 查询帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        // 查询帖子的作者
        User user = userService.findUserById(post.getUserId());
        // 帖子的点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
        // 帖子的点赞状态
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, post.getId());
        model.addAttribute("post", post);
        model.addAttribute("user", user);
        model.addAttribute("likeCount", likeCount);
        model.addAttribute("likeStatus", likeStatus);

        //评论分页信息
        page.setLimit(5);
        page.setRows(post.getCommentCount());
        page.setPath("/discuss/detail/" + discussPostId);

        // 评论: 给帖子的评论
        // 回复: 给评论的评论
        // 评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST,
                post.getId(), page.getOffset(), page.getLimit());

        //存放帖子评论的列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                //每一个帖子的评论包含：评论内容，评论作者，该评论的回复列表
                Map<String, Object> commentMap = new HashMap<>();
                //评论内容
                commentMap.put("comment", comment);
                //评论作者
                commentMap.put("user", userService.findUserById(comment.getUserId()));
                // 该条评论的点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentMap.put("likeCount", likeCount);
                // 该条评论的点赞状态
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(),
                                ENTITY_TYPE_COMMENT, comment.getId());
                commentMap.put("likeStatus", likeStatus);
                //评论的回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE
                );// 回复列表不需要分页
                List<Map<String, Object>> replyVoList = new ArrayList<>();//回复列表
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        //每个回复包含：回复内容，回复作者，回复的是哪个人
                        Map<String, Object> replyMap = new HashMap<>();
                        //回复内容
                        replyMap.put("reply", reply);
                        //回复作者
                        replyMap.put("user", userService.findUserById(reply.getUserId()));
                        //回复的是哪个人
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyMap.put("target", target);
                        // 该条回复的点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyMap.put("likeCount", likeCount);
                        // 该条回复的点赞状态
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(),
                                        ENTITY_TYPE_COMMENT, reply.getId());
                        replyMap.put("likeStatus", likeStatus);
                        replyVoList.add(replyMap);
                    }
                }
                //回复列表
                commentMap.put("replys", replyVoList);
                //回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentMap.put("replyCount", replyCount);

                commentVoList.add(commentMap);
            }
        }

        model.addAttribute("comments", commentVoList);
        return "site/discuss-detail";
    }
}
