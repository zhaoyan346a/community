package com.nowcoder.community.util;


import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommunityUtil {

    //生成随机字符串
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /*
    MD5加密
    hello -> abc123def456
    hello + 3e4a8 -> abc123def456abc
    给MD5加密后的字符串的尾部添加几位随机字符串可以降低密码被破解的概率
     */
    public static String md5(String key) {
        // org.apache.commons.lang3.StringUtils
        if (StringUtils.isBlank(key)) {
            return null;
        }
        // org.springframework.util.DigestUtils 生成MD5摘要
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

    public static String getJSONString(int code, String msg, Map<String, Object> map) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("msg", msg);
        if (map != null) {
            for (String key : map.keySet()) {
                jsonObject.put(key, map.get(key));
            }
        }
        return jsonObject.toJSONString();
    }

    public static String getJSONString(int code, String msg) {
        return getJSONString(code, msg, null);
    }

    public static String getJSONString(int code) {
        return getJSONString(code, null, null);
    }

    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "zss");
        map.put("age", 25);
        System.out.println(getJSONString(0, "ok", map));
    }
}
