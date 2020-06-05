package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    private ElasticsearchService elasticService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;

    /*
    请求路径： /search?keyword=xxx
     */
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        // 用es搜索帖子关键字
        org.springframework.data.domain.Page<DiscussPost> searchPosts =
                elasticService.searchDiscussPost(keyword, page.getCurrent(), page.getLimit());
        // 聚合数据：帖子、作者、点赞个数
        List<Map<String, Object>> posts = new ArrayList<>();
        if (searchPosts != null) {
            for (DiscussPost post : searchPosts) {
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", post);
                // 作者
                map.put("user", userService.findUserById(post.getUserId()));
                // 点赞数
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                posts.add(map);
            }
        }
        model.addAttribute("discussPosts", posts);
        model.addAttribute("keyword", keyword);//关键字重显

        //分页信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchPosts == null ? 0 : (int) searchPosts.getTotalElements());

        return "site/search";
    }
}
