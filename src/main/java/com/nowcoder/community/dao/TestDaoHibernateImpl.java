package com.nowcoder.community.dao;

import org.springframework.stereotype.Repository;

@Repository("hibernateDao")  //指定bean的唯一名字
public class TestDaoHibernateImpl implements TestDao {
    @Override
    public String select() {
        return "test dao";
    }
}
