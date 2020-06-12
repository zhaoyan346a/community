package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Value("${caffeine.posts.max-size}")
    private int maxSize;//缓存个数
    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;//缓存过期时间

    // Caffeine核心接口: Cache, LoadingCache, AsyncLoadingCache

    //帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    //帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize) //缓存数据个数
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS) //3分钟过期
                .build(new CacheLoader<String, List<DiscussPost>>() {//缓存中没有数据时，要从这里查找数据
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0)
                            throw new IllegalArgumentException("参数错误!");
                        String[] params = key.split(":");
                        if (params == null || params.length != 2)
                            throw new IllegalArgumentException("参数错误!");

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);
                        // 二级缓存: Redis -> mysql
                        logger.debug("load post list from DB.");
                        //查热帖
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize) //其实1个就够了
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)//过期时间
                .build(new CacheLoader<Integer, Integer>() {//缓存中没有数据时，要从这里查找数据
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    //查询讨论帖 orderMode: 0-->最新; 1-->最热
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        //只有查找热帖时才从缓存中取
        if (userId == 0 && orderMode == 1) {
            return postListCache.get(offset + ":" + limit);
        }
        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    //统计某个用户的讨论帖数量 0表示所有用户
    public int findDiscussPostRows(int userId) {
        if (userId == 0) {
            return postRowsCache.get(userId);
        }
        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    // 插入DiscussPost
    public int addDiscussPost(DiscussPost post) {
        if (post == null)
            throw new IllegalArgumentException("参数不能为空!");
        // 转义html标记  org.springframework.web.util.HtmlUtils.htmlEscape 可以把 < 转成 &lt;
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    // 查询特定id的讨论帖
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    //更新帖子评论数
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    //更新帖子类别  0-普通 1-置顶
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    //更新帖子状态  0-正常; 1-精华; 2-拉黑
    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    //更新帖子分数
    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
