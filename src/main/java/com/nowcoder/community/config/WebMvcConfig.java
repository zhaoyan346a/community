package com.nowcoder.community.config;

import com.nowcoder.community.controller.interceptor.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
多个拦截器的执行顺序：
preHandle按拦截器定义顺序调用
postHandler按拦截器定义逆序调用
afterCompletion按拦截器定义逆序调用
postHandler在拦截器链内所有拦截器返成功调用
afterCompletion只有preHandle返回true才调用
例如：interceptor1  interceptor2  顺序如下：
    方法执行前1
    方法执行前2
    方法执行后2
    方法执行后1
    页面渲染后2
    页面渲染后1
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private AlphaInterceptor alphaInterceptor;
    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;
    // 不用自定义注解，改用spring security控制权限
//    @Autowired
//    private LoginRequiredInterceptor loginRequiredInterceptor;
    @Autowired
    private MessageInterceptor messageInterceptor;
    @Autowired
    private DataInterceptor dataInterceptor;

    /*
    添加拦截器，拦截器的执行顺序与添加拦截器的顺序是相同的
    alphaInterceptor --> loginTicketInterceptor --> loginRequiredInterceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(alphaInterceptor)//测试连接器
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg")//不拦截静态资源
                .addPathPatterns("/register", "/login"); //测试拦截器只拦截这两个请求
        //拦截所有请求（除去excludePath里的路径）
        registry.addInterceptor(loginTicketInterceptor)//登陆凭证的拦截器
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
//        registry.addInterceptor(loginRequiredInterceptor)//登录拦截器
//                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
        registry.addInterceptor(messageInterceptor)//统计未读信息数拦截器
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
        registry.addInterceptor(dataInterceptor)//统计 UV DAU的拦截器
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
    }
}
