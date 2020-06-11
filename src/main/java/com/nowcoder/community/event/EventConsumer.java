package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    @Autowired
    private MessageService messageService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticsearchService elasticService;
    @Value("${wk.image.command}")
    private String wkImageCommand;//wk生成图片命令路径
    @Value("${wk.image.storage}")
    private String wkImageStorage;//wk存储图片路径

    //监听kafka的 点赞、关注、评论,实现系统通知
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_FOLLOW, TOPIC_LIKE})
    public void handleNoticeMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        //将发来的 json字符串转成 Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        //发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);//发起人ID
        message.setToId(event.getEntityUserId());//接收人ID
        message.setConversationId(event.getTopic());//会话ID为主题名
        message.setCreateTime(new Date());//发送时间

        //内容存储成json字符串形式
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("userId", event.getUserId());//事件触发人的id
        contentMap.put("entityType", event.getEntityType());//实体类型
        contentMap.put("entityId", event.getEntityId());//实体id
        //添加其余的事件信息
        if (!event.getMap().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getMap().entrySet()) {
                contentMap.put(entry.getKey(), entry.getValue());
            }
        }

        //内容存储成json字符串形式
        message.setContent(JSONObject.toJSONString(contentMap));
        messageService.addMessage(message);
    }

    // 监听发帖、给帖子评论的事件；目的：插入到elasticsearch中，用于搜索
    // 只是对帖子的搜索，不包括帖子中评论的搜索
    // 监听给帖子评论是因为会改变 帖子的“commentCount”，所以需要修改elasticsearch中的post
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        //将发来的 json字符串转成 Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticService.saveDiscussPost(post);//插入到elasticsearch中
    }

    //监听删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        //将发来的 json字符串转成 Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        //从es中删除
        elasticService.deleteDiscussPostById(event.getEntityId());
    }

    //监听(消费)分享图片事件
    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        //将发来的 json字符串转成 Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        //生成图片
        String htmlUrl = (String) event.getMap().get("htmlUrl");
        String fileName = (String) event.getMap().get("fileName");
        String suffix = (String) event.getMap().get("suffix");
        //wkhtmltoimage --quality 75 https://www.baidu.com F:/javaweb/projectSpace/data/wk-images/2.png
        String cmd = wkImageCommand + " --quality 75 " + htmlUrl + " " +
                wkImageStorage + "/" + fileName + suffix;

        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功: " + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: " + e.getMessage());
        }

        // 启用定时器,监视该图片,一旦生成了,则上传至七牛云.
//        UploadTask task = new UploadTask(fileName, suffix);
//        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
//        task.setFuture(future);

        /*class UploadTask implements Runnable {

            // 文件名称
            private String fileName;
            // 文件后缀
            private String suffix;
            // 启动任务的返回值
            private Future future;
            // 开始时间
            private long startTime;
            // 上传次数
            private int uploadTimes;

            public UploadTask(String fileName, String suffix) {
                this.fileName = fileName;
                this.suffix = suffix;
                this.startTime = System.currentTimeMillis();
            }

            public void setFuture(Future future) {
                this.future = future;
            }

            @Override
            public void run() {
                // 生成失败
                if (System.currentTimeMillis() - startTime > 30000) {
                    logger.error("执行时间过长,终止任务:" + fileName);
                    future.cancel(true);
                    return;
                }
                // 上传失败
                if (uploadTimes >= 3) {
                    logger.error("上传次数过多,终止任务:" + fileName);
                    future.cancel(true);
                    return;
                }

                String path = wkImageStorage + "/" + fileName + suffix;
                File file = new File(path);
                if (file.exists()) {
                    logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                    // 设置响应信息
                    StringMap policy = new StringMap();
                    policy.put("returnBody", CommunityUtil.getJSONString(0));
                    // 生成上传凭证
                    Auth auth = Auth.create(accessKey, secretKey);
                    String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                    // 指定上传机房  华北机房
                    UploadManager manager = new UploadManager(new Configuration(Zone.zone1()));
                    try {
                        // 开始上传图片
                        Response response = manager.put(
                                path, fileName, uploadToken, null, "image/" + suffix, false);
                        // 处理响应结果
                        JSONObject json = JSONObject.parseObject(response.bodyString());
                        if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                            logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                        } else {
                            logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                            future.cancel(true);
                        }
                    } catch (QiniuException e) {
                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    }
                } else {
                    logger.info("等待图片生成[" + fileName + "].");
                }
            }
        }*/
    }
}
