package com.nowcoder.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testStrings() {
        String redisKey = "test:count";
        //给key设置value
        redisTemplate.opsForValue().set(redisKey, 1);
        //获取一个key的value
        System.out.println(redisTemplate.opsForValue().get(redisKey));
        //key增加1
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        //key减少1
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHashes() {
        String redisKey = "test:user";
        //给hash设置 key value
        redisTemplate.opsForHash().put(redisKey, "id", 1);
        redisTemplate.opsForHash().put(redisKey, "username", "zhangsan");
        //查询hash下的某个key的value
        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));
    }

    @Test
    public void testLists() {
        String redisKey = "test:ids";
        //给列表左插value
        redisTemplate.opsForList().leftPush(redisKey, 101);
        redisTemplate.opsForList().leftPush(redisKey, 102);
        redisTemplate.opsForList().leftPush(redisKey, 103);
        //获取列表元素个数
        System.out.println(redisTemplate.opsForList().size(redisKey));
        //获取列表中某索引位置的值
        System.out.println(redisTemplate.opsForList().index(redisKey, 0));
        //获取列表某范围索引的值
        System.out.println(redisTemplate.opsForList().range(redisKey, 0, 2));
        //让列表左边弹出一个元素
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
    }

    @Test
    public void testSets() {
        String redisKey = "test:teachers";
        //给集合添加元素
        redisTemplate.opsForSet().add(redisKey, "刘备", "关羽", "张飞", "赵云", "诸葛亮");

        System.out.println(redisTemplate.opsForSet().size(redisKey));//集合元素个数
        System.out.println(redisTemplate.opsForSet().pop(redisKey));//弹出一个元素
        System.out.println(redisTemplate.opsForSet().members(redisKey));//返回集合元素
    }

    @Test
    public void testSortedSets() {
        String redisKey = "test:students";
        //有序集合添加元素
        redisTemplate.opsForZSet().add(redisKey, "唐僧", 80);
        redisTemplate.opsForZSet().add(redisKey, "悟空", 90);
        redisTemplate.opsForZSet().add(redisKey, "八戒", 50);
        redisTemplate.opsForZSet().add(redisKey, "沙僧", 70);
        redisTemplate.opsForZSet().add(redisKey, "白龙马", 60);

        //返回有序集合中的元素个数
        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
        //查询某个key的分数
        System.out.println(redisTemplate.opsForZSet().score(redisKey, "八戒"));
        //查询某个key的排名  升序
        System.out.println(redisTemplate.opsForZSet().rank(redisKey, "八戒"));
        //查询某个key的排名  降序
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "八戒"));
        //降序的范围查询
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey, 0, 2));
    }

    @Test
    public void testKeys() {
        //删除一个key
        redisTemplate.delete("test:user");
        //判断key是否存在
        System.out.println(redisTemplate.hasKey("test:user"));
        //给key设置过期时间
        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
    }

    // 批量发送命令,节约网络开销.
    @Test
    public void testBoundOperations() {
        String redisKey = "test:count";
        //绑定了一个key，然后就可以不写key了，减少代码量
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    // 编程式事务
    @Test
    public void testTransaction() {
        Object result = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String redisKey = "text:tx";

                // 启用事务
                redisOperations.multi();
                redisOperations.opsForSet().add(redisKey, "zhangsan");
                redisOperations.opsForSet().add(redisKey, "lisi");
                redisOperations.opsForSet().add(redisKey, "wangwu");
                // 因为开始事务后只是把命令放在了命令队列中，所以查询无效，是空的
                // 真正提交事务时才会执行命令队列中的命令
                System.out.println(redisOperations.opsForSet().members(redisKey));

                // 提交事务
                return redisOperations.exec();
            }
        });
        System.out.println(result);
    }

}
