package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;
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

import java.util.List;
import java.util.Map;


@Controller
public class FollowController implements CommunityConstant {
    @Autowired
    private FollowService followService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private EventProducer eventProducer;

    //关注
    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.follow(user.getId(), entityType, entityId);

        //触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)//设置关注主题
                .setUserId(hostHolder.getUser().getId())//发起人id
                .setEntityType(entityType)//实体类型：这里就是User
                .setEntityId(entityId)//实体id:这里就是被关注人的id
                .setEntityUserId(entityId);//实体对应的用户id:  就是entityId 因为只实现了关注人。
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0, "已关注!");
    }

    //取关
    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.unfollow(user.getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0, "已取消关注!");
    }

    //获取某个用户的正在关注的列表
    @RequestMapping(path = "/followes/{userId}", method = RequestMethod.GET)
    public String getFollowes(@PathVariable("userId") int userId, Page page, Model model) {
        //判断用户是否存在
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        //查询的是哪个用户的关注列表
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followes/" + userId);
        //设置该用户关注人的总数量
        page.setRows((int) followService.findFollowCount(userId, ENTITY_TYPE_USER));

        //查询正在关注的列表，包含：user(正在关注的人)，followTime(关注时间)
        List<Map<String, Object>> userList = followService.findFollowes(
                userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                //例如：自己是A，要查询B的正在关注列表(C D)，这里的hasFollowed是判断A是否关注了C D
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        // users包含：user(正在关注的人)，followTime(关注时间)，hasFollowed(自己是否关注了该user)
        model.addAttribute("users", userList);

        return "site/follow";
    }

    //获取某个用户的粉丝列表  和getFollowes方法一样，这里就不重构了
    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> userList = followService.findFollowers(
                userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);

        return "site/follower";
    }

    //判断当前登录用户是否关注了某用户
    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }
}
