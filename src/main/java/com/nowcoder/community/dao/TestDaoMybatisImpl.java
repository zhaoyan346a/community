package com.nowcoder.community.dao;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary  //当一个接口有多个实现类时，标有@Primary注解的类会被优先选择。
public class TestDaoMybatisImpl implements AlphaDao {
    @Override
    public String select() {
        return "mybatis test dao";
    }
}
