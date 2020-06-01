package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    @Autowired
    private MessageService messageService;

    //监听kafka的 点赞、关注、评论 主题
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_FOLLOW, TOPIC_LIKE})
    public void handleMessage(ConsumerRecord record) {
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
}
