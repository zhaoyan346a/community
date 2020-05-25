package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    //替换符
    private static final String REPLACEMENT = "***";
    //前缀树根节点
    private TrieNode trieRoot = new TrieNode();

    @PostConstruct //构造方法之后执行,初始化前缀树
    public void init() {
        try (
                InputStream in = this.getClass().getClassLoader().
                        getResourceAsStream("sensitive-words.txt");
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
        ) {
            String keyWord = null;
            while ((keyWord = br.readLine()) != null) {
                this.addKeyWord(keyWord);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败: " + e.getMessage());
        }
    }

    // 将一个敏感词添加到前缀树中
    private void addKeyWord(String keyWord) {
        if (StringUtils.isBlank(keyWord))
            return;
        TrieNode cur = trieRoot;
        for (int i = 0; i < keyWord.length(); i++) {
            char c = keyWord.charAt(i);
            if (cur.getNextNode(c) == null) {
                cur.addNode(c, new TrieNode());
            }
            cur = cur.getNextNode(c);
        }
        cur.setKeyWord(true);
    }

    /**
     * 过滤敏感词
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text))
            return null;

        TrieNode cur = trieRoot;// 指针1
        int begin = 0;// 指针2
        int end = 0;// 指针3
        StringBuilder sb = new StringBuilder();
        while (end < text.length()) {
            char c = text.charAt(end);

            if (isSymbol(c)) {//当前字符是特殊符号
                // 若指针1处于根节点,将此符号计入结果,让指针2向下走一步
                if (cur == trieRoot) {
                    sb.append(c);
                    begin++;
                }
                // 无论符号在开头或中间,指针3都向下走一步
                end++;
                continue;
            }

            // 检查下级节点
            cur = cur.getNextNode(c);
            if (cur == null) { // 以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                end = ++begin;// 进入下一个位置
                cur = trieRoot;// 重新指向根节点
            } else if (cur.isKeyWord()) {// 发现敏感词,将begin~position字符串替换掉
                sb.append(REPLACEMENT);
                begin = ++end;// 进入下一个位置
                cur = trieRoot;// 重新指向根节点
            } else {
                end++;// 检查下一个字符
            }
        }
        sb.append(text.substring(begin));// 将最后一批字符计入结果
        return sb.toString();
    }

    // 判断是否为特殊符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        // 不是ascii码 && 不是东亚文字  ==》 是特殊符号
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    //前缀树节点
    private class TrieNode {
        private boolean isKeyWord;// 关键词结束标识
        private Map<Character, TrieNode> next = new HashMap<>();// 子节点(key是下级字符,value是下级节点)

        // 获取子节点
        public TrieNode getNextNode(Character c) {
            return next.get(c);
        }

        // 添加子节点
        public void addNode(Character c, TrieNode node) {
            next.put(c, node);
        }

        public boolean isKeyWord() {
            return isKeyWord;
        }

        public void setKeyWord(boolean keyWord) {
            isKeyWord = keyWord;
        }
    }
}
