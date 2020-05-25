package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)//引入配置文件|配置类
public class MailTests {
    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail() {
        mailClient.sendMail("2142654560@qq.com", "TEST", "hello");
    }

    @Test
    public void testHTMLMail() {
        //获取thymeleaf的 org.thymeleaf.context.Context;
        Context context = new Context();
        context.setVariable("username", "zyandzss");

        // 解析指定路径下的模板，context用于设置上下文参数
        String content = templateEngine.process("mail/demo", context);
        System.out.println(content);
        mailClient.sendMail("2142654560@qq.com", "test html", content);
    }
}
