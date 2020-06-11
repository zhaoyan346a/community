package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)//引入配置文件|配置类
public class ElasticsearchTests {
    @Autowired
    private DiscussPostMapper discussMapper;
    @Autowired
    private DiscussPostRepository discussRepository;
    @Autowired
    private ElasticsearchTemplate elasticTemplate;

    //插入单个对象
    @Test
    public void testInsert() {
        discussRepository.save(discussMapper.selectDiscussPostById(241));
        discussRepository.save(discussMapper.selectDiscussPostById(242));
        discussRepository.save(discussMapper.selectDiscussPostById(243));
    }

    //插入多个对象 如List
    @Test
    public void testInsertList() {
        discussRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100, 0));
    }

    //修改
    @Test
    public void testUpdate() {
        //修改之后再save。
        DiscussPost discussPost = discussMapper.selectDiscussPostById(231);
        discussPost.setContent("我是新人,使劲灌水.");
        discussRepository.save(discussPost);
    }

    //删除
    @Test
    public void testDelete() {
        //discussRepository.deleteById(231);//删除主键为231的对象
        discussRepository.deleteAll();//删除所有对象
    }

    //用DiscussPostRepository  search时是没有给高亮的field进行处理的
    @Test
    public void testSearchByRepository() {
        //org.springframework.data.elasticsearch.core.query;
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                //查询条件
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                //排序条件：1、type降序 2、score降序 3、createTime降序
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //分页条件：当前页数(从0开始),每页记录的个数
                .withPageable(PageRequest.of(0, 10))
                //高亮显示(在前台给em标签添加css样式了，显示成红色)
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        //org.springframework.data.domain;
        Page<DiscussPost> page = discussRepository.search(searchQuery);
        System.out.println(page.getTotalElements());//记录总数
        System.out.println(page.getTotalPages());//总页数
        System.out.println(page.getNumber());//当前页  从0开始的
        System.out.println(page.getSize());//每页记录的个数
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }

    //用ElasticsearchTemplate 自己进行高亮field的处理
    @Test
    public void testSearchByTemplate() {
        //org.springframework.data.elasticsearch.core.query;
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                //查询条件
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                //排序条件：1、type降序 2、score降序 3、createTime降序
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //分页条件：当前页数(从0开始),每页记录的个数
                .withPageable(PageRequest.of(0, 10))
                //高亮显示(在前台给em标签添加css样式了，显示成红色)
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        Page<DiscussPost> page = elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                SearchHits hits = searchResponse.getHits();
                if (hits.getTotalHits() <= 0) {//没有数据返回null
                    return null;
                }
                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) {
                    DiscussPost post = new DiscussPost();
                    //以map形式返回
                    Map<String, Object> map = hit.getSourceAsMap();
                    //先都转成string
                    String id = map.get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = map.get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = map.get("title").toString();
                    post.setTitle(title);

                    String content = map.get("content").toString();
                    post.setContent(content);

                    String type = map.get("type").toString();
                    post.setType(Integer.valueOf(type));

                    String status = map.get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    //es存储Date时会转成Long类型
                    String createTime = map.get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String score = map.get("score").toString();
                    post.setScore(Double.valueOf(score));

                    //对高亮字段特殊处理
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null) {
                        //因为可能有多段匹配；只要第一段就行，不全部显示
                        post.setTitle(titleField.getFragments()[0].toString());
                    }
                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        post.setContent(contentField.getFragments()[0].toString());
                    }
                    list.add(post);
                }
                // 这个不懂，固定用法
                return new AggregatedPageImpl(list, pageable, hits.getTotalHits(),
                        searchResponse.getAggregations(), searchResponse.getScrollId(),
                        hits.getMaxScore());
            }
        });
        System.out.println(page.getTotalElements());//记录总数
        System.out.println(page.getTotalPages());//总页数
        System.out.println(page.getNumber());//当前页  从0开始的
        System.out.println(page.getSize());//每页记录的个数
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }
}
