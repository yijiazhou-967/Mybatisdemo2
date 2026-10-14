package com.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

/** 部门/员工示例的会话工厂；SqlSession 用完必须关闭，修改后显式提交。 */
public final class MyBatisUtil {
    private static final SqlSessionFactory FACTORY = buildFactory();

    private MyBatisUtil() {
    }

    private static SqlSessionFactory buildFactory() {
        Properties overrides = new Properties();
        URL local = MyBatisUtil.class.getClassLoader().getResource("db-local.properties");
        try (InputStream config = Resources.getResourceAsStream("mybatis-config.xml")) {
            if (local != null) {
                try (InputStream input = local.openStream()) {
                    overrides.load(new InputStreamReader(input, StandardCharsets.UTF_8));
                }
            }
            for (String key : new String[]{"driver", "url", "username", "password"}) {
                String environment = System.getenv("DEPT_EMP_DB_" + key.toUpperCase(Locale.ROOT));
                if (environment != null) {
                    overrides.setProperty("db." + key, environment);
                }
                String system = System.getProperty("db." + key);
                if (system != null) {
                    overrides.setProperty("db." + key, system);
                }
            }
            return new SqlSessionFactoryBuilder().build(config, overrides);
        } catch (IOException e) {
            throw new IllegalStateException("加载部门/员工 MyBatis 配置失败", e);
        }
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        return FACTORY;
    }

    public static SqlSession openSession() {
        return FACTORY.openSession();
    }
}
