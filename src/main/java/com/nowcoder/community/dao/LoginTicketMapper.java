package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
@Deprecated //不推荐使用了(登录凭证是添加到mysql中的，效率低)
public interface LoginTicketMapper {

    @Insert({"insert into login_ticket(user_id,ticket,status,expired) ",
            "values(#{userId},#{ticket},#{status},#{expired})"})
    //自动生成主键，指定主键属性id
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({"select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"})
    LoginTicket selectByTicket(String ticket);

    //如果需要拼接动态SQL，可以参考下面的写法
    @Update({
            "<script>",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\">",
            "and 1=1 ",
            "</if>",
            "</script>"
    })
    int updateStatus(String ticket, int status);
}
