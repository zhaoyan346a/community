package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Value("${community.path.domain}")
    private String domain;//域名  http://localhost:8080
    @Value("${server.servlet.context-path}")
    private String contextPath;//应用名称  /community

    @Autowired
    private TemplateEngine templateEngine;// thymeleaf模板引擎

    @Autowired
    private MailClient mailClient;//发邮件的工具类

    @Autowired
    private RedisTemplate redisTemplate;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    public User findUserById(int id) {
//        return userMapper.selectById(id);
        User user = getCache(id);//看缓存中是否有用户
        if (user == null) {//缓存里没有
            user = initCache(id);//从mysql中查并添加到redis中
        }
        return user;
    }

    //用户注册功能
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if (user == null)
            throw new IllegalArgumentException("参数不能为空");
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        //判断是否已经存在 账号或邮箱
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));//设置5位随机字符
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));//md5(password+salt)
        user.setType(0);//普通用户
        user.setStatus(0);//未激活
        user.setActivationCode(CommunityUtil.generateUUID());//设置激活码
        //http://images.nowcoder.com/head/1t.png  牛客网上的图片地址
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        //由模板引擎处理页面(mail/activation)后转成字符串content
        String content = templateEngine.process("mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    //激活用户功能
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ACTIVATION_FAILURE;
        } else {
            if (user.getStatus() == 1) {
                return ACTIVATION_REPEAT;//重复激活
            } else if (user.getActivationCode().equals(code)) {
                userMapper.updateStatus(userId, 1);//修改了用户状态
                clearCache(userId);//清除redis中的缓存用户
                return ACTIVATION_SUCCESS;//激活成功
            } else {
                return ACTIVATION_FAILURE;//激活失败
            }
        }
    }

    //登录功能
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();
        //判断空值
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(username)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("passwordMsg", "该账号不存在!");
            return map;
        }
        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }
        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        // 每次登录都会生成一个登录凭证，根据某用户的登录凭证的个数可以统计他登录了几次
        // 统计：最早注册时间、最早登录时间、一年访问了多少天
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());//登录用户的id
        loginTicket.setTicket(CommunityUtil.generateUUID());//设置登录凭证的一个随机字符串
        loginTicket.setStatus(0);//0表示有效
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));//设置过期时间
//        loginTicketMapper.insertLoginTicket(loginTicket);
        //不往mysql里面插入了，往redis里面插入
        //获取登陆凭证的key
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        //这里loginTicket是一个对象，但是在RedisConfig里面已经设置过value的序列化方式为json
        //所以会将loginTicket序列化为json字符串
        redisTemplate.opsForValue().set(redisKey, loginTicket);

        map.put("ticket", loginTicket.getTicket());//登录成功才会有ticket
        return map;
    }

    //登出功能
    public void logout(String ticket) {
//        loginTicketMapper.updateStatus(ticket, 1);//1 无效
        //获取登陆凭证的key
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        //在redis中获取登陆凭证
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);//设置成1 登录凭证无效
        redisTemplate.opsForValue().set(redisKey, loginTicket);//在redis中更新登陆凭证
    }

    //根据ticket查找LoginTicket
    public LoginTicket findLoginTicket(String ticket) {
//        return loginTicketMapper.selectByTicket(ticket);
        //获取登陆凭证的key
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        //在redis中获取登陆凭证
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    //更新用户头像地址
    public int updateHeader(int userId, String headerUrl) {
//        return userMapper.updateHeader(userId, headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);//修改了用户状态
        clearCache(userId);//清除redis中的缓存用户
        return rows;
    }

    //修改用户密码
    public int updatePassword(int userId, String password) {
        return userMapper.updatePassword(userId, password);
    }

    //根据用户名查用户
    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    // 1.优先从缓存中取值(看redis中是否有用户)
    private User getCache(int userId) {
        //获取用户对应的key
        String redisKey = RedisKeyUtil.getUserKey(userId);
        //返回redis中的用户
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.取不到时初始化缓存数据(给redis添加用户)
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        //一个小时过期
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据(清楚redis中的用户)
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }
}
