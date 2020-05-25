package com.nowcoder.community;

import com.nowcoder.community.dao.AlphaDao;
import com.nowcoder.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)//引入配置文件|配置类
class CommunityApplicationTests implements ApplicationContextAware {
    private ApplicationContext applicationContext;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testApplicationContext() {
        System.out.println(applicationContext);
        AlphaDao dao = applicationContext.getBean(AlphaDao.class);
        System.out.println(dao.select());

        dao = applicationContext.getBean("hibernateDao", AlphaDao.class);
        System.out.println(dao.select());
    }

    @Test
    public void testBeanManagement() {
        AlphaService service = applicationContext.getBean(AlphaService.class);
        System.out.println(service);
    }

    @Test
    public void testBeanConfig() {
        SimpleDateFormat simpleDateFormat = applicationContext.getBean(SimpleDateFormat.class);
        System.out.println(simpleDateFormat.format(new Date()));
    }

    @Autowired //注入需要的bean
    @Qualifier("hibernateDao") //如果该类型的bean有多个实现类，用@Qualifier注解指定要注入的bean
    private AlphaDao testDao;

    @Autowired
    private AlphaService testService;

    @Autowired
    private SimpleDateFormat simpleDateFormat;

    @Test
    public void testDI() {
        System.out.println(testDao);
        System.out.println(testService);
        System.out.println(simpleDateFormat);
    }
}
