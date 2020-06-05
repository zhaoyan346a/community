package com.nowcoder.community.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {
    @Autowired
    private DiscussPostRepository discussRepository;
    @Autowired
    private ElasticsearchTemplate elasticTemplate;

    //往elasticsearch里插入一个post
    public void saveDiscussPost(DiscussPost post) {
        discussRepository.save(post);
    }

    //删除一个post
    public void deleteDiscussPostById(int id) {
        discussRepository.deleteById(id);
    }

    /**
     * 根据关键字分页查询post
     *
     * @param keyword 查询关键字
     * @param current 页码
     * @param limit   每页个数
     * @return
     */
    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        //org.springframework.data.elasticsearch.core.query;
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                //查询条件
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                //排序条件：1、type降序 2、score降序 3、createTime降序
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //分页条件：当前页数(从0开始),每页记录的个数
                .withPageable(PageRequest.of(current - 1, limit))
                //高亮显示(在前台给em标签添加css样式了，显示成红色)
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        return elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
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

                    String commentCount = map.get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

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
    }
}
