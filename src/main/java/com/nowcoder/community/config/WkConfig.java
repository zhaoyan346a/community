package com.nowcoder.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
public class WkConfig {
    private static final Logger logger = LoggerFactory.getLogger(WkConfig.class);
    @Value("${wk.image.storage}")
    private String wkImageStorage;//wk图片存储路径

    @PostConstruct
    public void init() {
        File file = new File(wkImageStorage);
        if (!file.exists()) {//图片目录不存在
            file.mkdirs();
            logger.info("创建WK图片目录: " + wkImageStorage);
        }
    }
}
