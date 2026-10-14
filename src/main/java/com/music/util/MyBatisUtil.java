package com.music.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * MyBatis 会话工厂工具类。
 * SqlSessionFactory 全局仅需构建一次，避免重复读取配置。
 */
public final class MyBatisUtil {

    private static final SqlSessionFactory SQL_SESSION_FACTORY;

    static {
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) {
            Properties overrides = new Properties();
            // 本地口令保存在被 Git 忽略的文件中，保留原有开发连接配置。
            URL local = MyBatisUtil.class.getClassLoader().getResource("jdbc-local.properties");
            if (local != null) {
                try (InputStream localIn = local.openStream()) {
                    overrides.load(new InputStreamReader(localIn, StandardCharsets.UTF_8));
                }
            }
            String[] keys = {"driver", "url", "username", "password"};
            for (String key : keys) {
                String env = System.getenv("MUSIC_DB_" + key.toUpperCase(java.util.Locale.ROOT));
                if (env != null) {
                    overrides.setProperty("jdbc." + key, env);
                }
                String system = System.getProperty("jdbc." + key);
                if (system != null) {
                    overrides.setProperty("jdbc." + key, system);
                }
            }
            SQL_SESSION_FACTORY = new SqlSessionFactoryBuilder().build(in, overrides);
        } catch (IOException e) {
            throw new RuntimeException("加载 mybatis-config.xml 失败", e);
        }
    }

    /** 工具类禁止实例化 */
    private MyBatisUtil() {
    }

    /** 访问共享工厂，可检查映射配置，无需建立数据库连接。 */
    public static SqlSessionFactory getSqlSessionFactory() {
        return SQL_SESSION_FACTORY;
    }

    /**
     * 开启一个新的 SqlSession（非线程安全，用完必须关闭）。
     */
    public static SqlSession openSession() {
        return SQL_SESSION_FACTORY.openSession();
    }
}
