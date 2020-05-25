package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//自定义注解一般就是以下四个注解的组合了。
//@Document 描述该注解是否可以生成到文档中
//@Inherited 类继承关系中，子类会继承父类使用的注解中被@Inherited修饰的注解
@Target(ElementType.METHOD)  //Target用于描述该注解可以作用的目标类型（如：用在方法上）
@Retention(RetentionPolicy.RUNTIME) //Retention描述该注解被保留的时间（如：运行时有效）
public @interface LoginRequired {
}
