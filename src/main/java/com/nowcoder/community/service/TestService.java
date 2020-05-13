package com.nowcoder.community.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class TestService {
    public TestService(){
        System.out.println("实例化testService");
    }

    @PostConstruct
    public void init(){
        System.out.println("初始化testService");
    }
    @PreDestroy
    public void destroy(){
        System.out.println("销毁testService");
    }
}
