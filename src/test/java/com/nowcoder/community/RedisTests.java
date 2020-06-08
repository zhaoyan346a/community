package com.nowcoder.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
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

    //HyperLogLog给20万个数据去重
    // HyperLogLog占用空间小，但是有误差(大概0.81%)
    @Test
    public void testHyperLogLog() {
        String redisKey = "test:hll:01";
        for (int i = 1; i <= 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey, i);
        }

        for (int i = 1; i <= 100000; i++) {
            int r = (int) (Math.random() * 100000 + 1);
            redisTemplate.opsForHyperLogLog().add(redisKey, r);
        }
        long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size);//本来该100000，但是输出99553
    }

    //用HyperLogLog将3组数据合并后再去重
    @Test
    public void testHyperLogLogUnion() {
        String redisKey2 = "test:hll:02";
        for (int i = 1; i <= 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2, i);
        }

        String redisKey3 = "test:hll:03";
        for (int i = 5001; i <= 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3, i);
        }

        String redisKey4 = "test:hll:04";
        for (int i = 10000; i <= 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey4, i);
        }

        String unionKey = "test:hll:union";
        //hyperloglog 合并多组数据
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4);
        long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        System.out.println(size);//本来该20000， 但是输出19833
    }

    //用bitmap统计一组数据（去重）
    @Test
    public void testBitMap() {
        String redisKey = "test:bm:01";

        //记录
        redisTemplate.opsForValue().setBit(redisKey, 1, true);
        redisTemplate.opsForValue().setBit(redisKey, 4, true);
        redisTemplate.opsForValue().setBit(redisKey, 7, true);
        //查询
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));//false
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));//true
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));//false
        //统计
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes());//统计某个redisKey的bit个数
            }
        });
        System.out.println(obj);
    }

    //用bitmap统计多组数据，并进行OR运算
    @Test
    public void testBitMapOperation() {
        String redisKey2 = "test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey2, 0, true);
        redisTemplate.opsForValue().setBit(redisKey2, 1, true);
        redisTemplate.opsForValue().setBit(redisKey2, 2, true);

        String redisKey3 = "test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey3, 2, true);
        redisTemplate.opsForValue().setBit(redisKey3, 3, true);
        redisTemplate.opsForValue().setBit(redisKey3, 4, true);

        String redisKey4 = "test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey4, 4, true);
        redisTemplate.opsForValue().setBit(redisKey4, 5, true);
        redisTemplate.opsForValue().setBit(redisKey4, 6, true);

        String redisKey = "test:bm:or";

        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                connection.bitOp(RedisStringCommands.BitOperation.OR, // or运算
                        redisKey.getBytes(), //目标bitmap
                        redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());//源bitmap
                return connection.bitCount(redisKey.getBytes());
            }
        });

        System.out.println(obj);

        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 3));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 4));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 5));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 6));
    }
}
