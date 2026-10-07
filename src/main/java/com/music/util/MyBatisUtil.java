package com.music.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * MyBatis 会话工厂工具类。
 * SqlSessionFactory 全局仅需构建一次，避免重复读取配置。
 */
public final class MyBatisUtil {

    private static final SqlSessionFactory SQL_SESSION_FACTORY;

    static {
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) {
            SQL_SESSION_FACTORY = new SqlSessionFactoryBuilder().build(in);
        } catch (IOException e) {
            throw new RuntimeException("加载 mybatis-config.xml 失败", e);
        }
    }

    /** 工具类禁止实例化 */
    private MyBatisUtil() {
    }

    /**
     * 开启一个新的 SqlSession（非线程安全，用完必须关闭）。
     */
    public static SqlSession openSession() {
        return SQL_SESSION_FACTORY.openSession();
    }
}
