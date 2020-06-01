package com.nowcoder.community.entity;

import java.util.HashMap;
import java.util.Map;

// 事件对象
public class Event {
    private String topic;//事件类型，对kafka来说就是主题
    private int userId;//触发事件的用户id
    private int entityType;//实体类型
    private int entityId;//实体id
    private int entityUserId;//实体的作者id
    private Map<String, Object> map = new HashMap<>();//存储事件需要的其他键值对

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public Event setData(String key, Object val) {
        this.map.put(key, val);
        return this;
    }
}
