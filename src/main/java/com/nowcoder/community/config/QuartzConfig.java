package com.nowcoder.community.config;

import com.nowcoder.community.quartz.AlphaJob;
import com.nowcoder.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

//QuartzConfig
//如果没对Quartz进行自定义配置，那么就会读取内存中的默认配置执行任务，而不是读取数据库中的配置。
//如果对Quartz进行了自定义配置（application.yml或properties），jobStore要求存到数据库，配置 -> 数据库 -> 调用
//在项目第一次启动时才会将配置信息写到数据库中，之后Quartz就在数据库中读信息执行任务了
@Configuration
public class QuartzConfig {

    // FactoryBean可简化Bean的实例化过程:
    // 1.通过FactoryBean封装Bean的实例化过程.
    // 2.将FactoryBean装配到Spring容器里.
    // 3.将FactoryBean注入给其他的Bean.
    // 4.该Bean得到的是FactoryBean所管理的对象实例.

    //配置JobDetail
    //@Bean
    public JobDetailFactoryBean alphaJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class);//任务的class
        factoryBean.setName("alphaJob");//任务名
        factoryBean.setGroup("alphaJobGroup");//任务组
        factoryBean.setDurability(true);//可持久化
        factoryBean.setRequestsRecovery(true);//请求可恢复
        return factoryBean;
    }

    // 配置Trigger(SimpleTriggerFactoryBean, CronTriggerFactoryBean)
    //@Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail);//传入JobDetail
        factoryBean.setName("alphaTrigger");//trigger名
        factoryBean.setGroup("alphaTriggerGroup");//trigger组名
        factoryBean.setRepeatInterval(3000);//执行频率 3000ms执行一次
        factoryBean.setJobDataMap(new JobDataMap());//存储Job状态
        return factoryBean;
    }

    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);//任务的class
        factoryBean.setName("postScoreRefreshJob");//任务名
        factoryBean.setGroup("communityJobGroup");//任务组
        factoryBean.setDurability(true);//可持久化
        factoryBean.setRequestsRecovery(true);//请求可恢复
        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);//传入JobDetail
        factoryBean.setName("postScoreRefreshTrigger");//trigger名
        factoryBean.setGroup("communityTriggerGroup");//trigger组名
        factoryBean.setRepeatInterval(1000 * 60 * 5);//执行频率 5分钟执行一次(实际可以设置成1小时一次)
        factoryBean.setJobDataMap(new JobDataMap());//存储Job状态
        return factoryBean;
    }


}
