package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {
    @Autowired
    private CommentService commentService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        // comment里面已经有了前台表单传过来的 content,entityType,entityId; 也有可能有targetId
        // 如果没有targetId，默认为0
        comment.setUserId(hostHolder.getUser().getId());//评论发起人id
        comment.setStatus(0);//0表示有效
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        //触发评论事件(系统通知)
        Event event = new Event()
                .setTopic(TOPIC_COMMENT) //设置评论主题
                .setEntityType(comment.getEntityType()) //实体类型
                .setEntityId(comment.getEntityId()) //实体ID
                .setUserId(hostHolder.getUser().getId()) //事件发起人ID
                .setData("postId", discussPostId); //事件额外数据，对于评论来说就是帖子的id，查看详情使用
        if (comment.getEntityType() == ENTITY_TYPE_POST) { //评论的是帖子
            DiscussPost target = discussPostService.findDiscussPostById(discussPostId);
            event.setEntityUserId(target.getUserId());//实体对应的用户id为：帖子对应的用户id
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) { //评论的是帖子中的回复
            Comment target = commentService.findCommentById(comment.getEntityId());//实体对应的用户id为：评论对应的用户id
            event.setEntityUserId(target.getUserId());
        }
        //事件生产者发送事件
        eventProducer.fireEvent(event);

        //触发TOPIC_PUBLISH事件
        if (comment.getEntityType() == ENTITY_TYPE_POST) {//给帖子评论，改变了commentCount
            //只用得到 topic 和 entityId
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
        }

        return "redirect:/discuss/detail/" + discussPostId;
    }
}
