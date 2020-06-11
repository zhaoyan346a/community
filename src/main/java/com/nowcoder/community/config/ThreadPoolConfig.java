package com.nowcoder.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


//配置了这个类，才可以使用Spring的ThreadPoolTaskScheduler 定时线程池类
@Configuration
@EnableScheduling  //主要是这个注解
@EnableAsync// 启用 @Async注解
public class ThreadPoolConfig {
}
