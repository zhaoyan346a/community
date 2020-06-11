package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {
    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    //统计uv 访问ip插入当天uv的redisKey中
    public void recordUV(String ip) {
        //当天uvKey
        String uvKey = RedisKeyUtil.getUVKey(format.format(new Date()));
        //插入
        redisTemplate.opsForHyperLogLog().add(uvKey, ip);
    }

    //将用户id插入当天的DAU
    public void recordDAU(int userId) {
        //当天dauKey
        String dauKey = RedisKeyUtil.getDAUKey(format.format(new Date()));
        //设置userId位置处为true
        redisTemplate.opsForValue().setBit(dauKey, userId, true);
    }

    //统计指定日期范围的UV
    public long caculateUV(Date startDate, Date endDate) {
        if (startDate == null || endDate == null)
            throw new IllegalArgumentException("参数不能为空");
        //整理该日期范围的key
        List<String> uvKeyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        while (!calendar.getTime().after(endDate)) {//统计日期<=endDate
            //将这一天的uvKey添加到List
            uvKeyList.add(RedisKeyUtil.getUVKey(format.format(calendar.getTime())));
            //日期加1
            calendar.add(Calendar.DATE, 1);
        }
        //日期范围uvKey
        String resultKey = RedisKeyUtil.getUVKey(format.format(startDate), format.format(endDate));
        //合并这些数据
        redisTemplate.opsForHyperLogLog().union(resultKey, uvKeyList.toArray());
        //返回统计的结果
        return redisTemplate.opsForHyperLogLog().size(resultKey);
    }

    //统计指定日期范围的DAU
    public long caculateDAU(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        List<byte[]> dauKeyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        while (!calendar.getTime().after(endDate)) {
            //将这一天的dauKey添加到List
            String dauKey = RedisKeyUtil.getDAUKey(format.format(calendar.getTime()));
            dauKeyList.add(dauKey.getBytes());
            calendar.add(Calendar.DATE, 1);//日期加1
        }
        //进行OR运算
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String resultKey = RedisKeyUtil.getUVKey(format.format(startDate), format.format(endDate));
                //OR
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        resultKey.getBytes(), dauKeyList.toArray(new byte[0][0]));
                return connection.bitCount(resultKey.getBytes());//返回结果
            }
        });
    }
}
