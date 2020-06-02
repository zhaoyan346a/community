package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
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
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    // 私信列表
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
        User user = hostHolder.getUser();
        // 分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));

        // 会话列表
        List<Message> conversationList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList != null) {
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                //会话内容
                map.put("conversation", message);
                //会话中的总信息数
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                //会话中未读信息数
                map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                //会话的另一个用户
                map.put("target", userService.findUserById(targetId));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);

        // 查询私信未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        // 查询通知未读消息数量
        int noticeUnreadCount = messageService.selectNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "site/letter";
    }

    //会话的消息详情
    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        // 分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        //会话的私信数量
        page.setRows(messageService.findLetterCount(conversationId));

        // 私信列表
        //查询用户的一个会话的所有内容
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                //私信内容
                map.put("letter", message);
                //私信发送人
                map.put("fromUser", userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        // letters 包含：letter 私信, fromUser 私信发送人
        model.addAttribute("letters", letters);

        // 会话的另一个用户
        model.addAttribute("target", getLetterTarget(conversationId));

        // 将未读的消息设置为已读
        List<Integer> ids = getLetterIds(letterList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "site/letter-detail";
    }

    //返回会话的另一个用户
    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);

        if (hostHolder.getUser().getId() == id0) {
            return userService.findUserById(id1);
        } else {
            return userService.findUserById(id0);
        }
    }

    // 将未读的消息设置为已读
    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        if (letterList != null) {
            for (Message message : letterList) {
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }

    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content) {
        //根据用户名查询用户
        User target = userService.findUserByName(toName);
        if (target == null) {//私信接收者不存在
            return CommunityUtil.getJSONString(1, "目标用户不存在!");
        }
        //构建私信
        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        // 会话id = 小id_大id
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0, "发送成功");
    }

    // 系统通知列表
    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    @LoginRequired
    public String getNoticeList(Model model) {
        User user = hostHolder.getUser();
        //查询评论、点赞、关注等通知的最新的一条通知
        String[] notices = new String[]{TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW};
        for (String topic : notices) {
            //最新的一条通知
            Message message = messageService.selectLatestNotice(user.getId(), topic);
            Map<String, Object> map = new HashMap<>();
            map.put("message", null);
            if (message != null) {//存在通知
                map.put("message", message);//message是通知对象
                String content = message.getContent();
                content = HtmlUtils.htmlUnescape(content);//将特殊符号正常显示
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);//json字符串转成Map
                map.put("user", userService.findUserById((Integer) data.get("userId")));//发起人
                map.put("entityType", data.get("entityType"));//实体类型
                map.put("entityId", data.get("entityId"));//实体id
                map.put("postId", data.get("postId"));//帖子id
                //该类通知总数
                int totalCount = messageService.selectNoticeCount(user.getId(), topic);
                map.put("totalCount", totalCount);
                //该类通知未读数
                int unreadCount = messageService.selectNoticeUnreadCount(user.getId(), topic);
                map.put("unreadCount", unreadCount);
            }
            // commentNotice: message,user,entityType,entityId,postId,totalCount,unreadCount
            // likeNotice: 同上
            // followNotice: 同上
            model.addAttribute(topic + "Notice", map);
        }
        // 查询私信未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        // 查询通知未读消息数量
        int noticeUnreadCount = messageService.selectNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        return "site/notice";
    }

    // 通知详情列表
    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    @LoginRequired
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) {
        User user = hostHolder.getUser();
        //设置分页信息
        page.setLimit(5);//默认为10
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.selectNoticeCount(user.getId(), topic));
        //获得一页的通知列表
        List<Message> messageList = messageService.selectNotices(user.getId(), topic,
                page.getOffset(), page.getLimit());
        List<Map<String, Object>> messageVOList = new ArrayList<>();
        if (messageList != null) {
            for (Message message : messageList) {
                Map<String, Object> map = new HashMap<>();
                map.put("message", message);//通知
                String content = message.getContent();
                content = HtmlUtils.htmlUnescape(content);//将特殊符号正常显示
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);//json字符串转成Map
                map.put("user", userService.findUserById((Integer) data.get("userId")));//发起人
                map.put("entityType", data.get("entityType"));//实体类型
                map.put("entityId", data.get("entityId"));//实体id
                map.put("postId", data.get("postId"));//帖子id
                map.put("fromUser", userService.findUserById(message.getFromId()));//fromUser就是系统用户SYSTEM
                messageVOList.add(map);
            }
        }
        //notices: message,user,entityType,entityId,postId,fromUser
        model.addAttribute("notices", messageVOList);
        // 将未读的消息设置为已读
        List<Integer> ids = getLetterIds(messageList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        return "site/notice-detail";
    }
}
