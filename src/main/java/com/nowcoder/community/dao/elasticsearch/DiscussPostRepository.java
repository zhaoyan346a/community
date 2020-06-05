package com.nowcoder.community.dao.elasticsearch;

import com.nowcoder.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

// ElasticsearchRepository<DiscussPost, Integer> 操作的数据类型是 DiscussPost, 主键类型是 Integer
// DiscussPostRepository 可以直接使用一些 save delete search 等方法。
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {

}
