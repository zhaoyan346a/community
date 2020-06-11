package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.nowcoder.community.entity.User;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

@Service
public class AlphaService {
    private static final Logger logger = LoggerFactory.getLogger(AlphaService.class);
    @Autowired
    private AlphaDao alphaDao;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired //由spring自动创建并注入到ioc容器中
    private TransactionTemplate transactionTemplate;

    public AlphaService() {
        System.out.println("实例化testService");
    }

    @PostConstruct
    public void init() {
        System.out.println("初始化testService");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("销毁testService");
    }

    public String find() {
        return alphaDao.select();
    }

    /*
    spring的传播级别有7个：假如在事务A里调用 事务B方法，对于事务B来说事务A就是当前事务（外部事务）
    前三个是常用的
    1、REQUIRED: 支持当前事务（外部事务），即当前事务存在就用当前事务，如果不存在，就创建新事务；
    2、REQUIRES_NEW：创建一个新事务，并暂停当前事务（外部事务）
    3、NESTED：如果当前事务A（外部事务）存在，则嵌套在该事务中执行（有独立的提交和回滚）；
               如果当前事务不存在，则创建新事务。
    4、NEVER：不支持事务；如果存在事务就会抛异常。
    5、SUPPORTS：支持当前事务，当前事务存在就用当前事务，如果不存在，也不会创建新事务；
    6、NOT_SUPPORTED：不支持当前事务，如果当前事务存在则暂时当前事务，然后以非事务方式执行。
    7、MANDATORY （强制的）：支持当前事务，如果当前事务不存在就抛异常。
    支持当前事务：REQUIRED  NESTED   SUPPORTS  MANDATORY
    不支持当前事务：REQUIRES_NEW  NEVER  NOT_SUPPORTED
     */
    /*
    spring的隔离级别有五个：
    DEFAULT（使用底层数据库的隔离级别，这是spring的默认隔离级别）
    READ_UNCOMMITTED  READ_COMMITTED  REPEATABLE_READ  SERIALIZABLE
     */
    // 1、声明式事务
    @Transactional(isolation = Isolation.DEFAULT, propagation = Propagation.REQUIRED)
    public Object save1() {
        // 新增用户
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
        user.setEmail("alpha@qq.com");
        user.setHeaderUrl("http://image.nowcoder.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);
        // 新增帖子
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle("Hello");
        post.setContent("新人报道!");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(post);

        Integer.valueOf("abc");
        return "ok";
    }

    // 2、编程式事务
    public Object save2() {
        // 设置隔离级别和传播行为
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_DEFAULT);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        // 事务模板执行一个自定义的回调方法
        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                // 新增用户
                User user = new User();
                user.setUsername("beta");
                user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
                user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
                user.setEmail("beta@qq.com");
                user.setHeaderUrl("http://image.nowcoder.com/head/999t.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);
                // 新增帖子
                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId());
                post.setTitle("你好");
                post.setContent("我是新人!");
                post.setCreateTime(new Date());
                discussPostMapper.insertDiscussPost(post);

                Integer.valueOf("abc");
                return "ok";
            }
        });
    }

    //@Async注解让该方法在多线程环境下可以被异步调用,使用Spring内置的线程池
    @Async
    public void execute1(){
        logger.debug("execute1");
    }

    //@Scheduled注解让该方法可以被Spring内置的定时线程池调用， 延迟10秒，每一秒执行1次 需要@EnableScheduling
    //@Scheduled(initialDelay = 10000,fixedRate = 1000)
    public void execute2(){
        logger.debug("execute2");
    }
}
