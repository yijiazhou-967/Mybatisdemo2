package com.console;

import com.util.MyBatisUtil;
import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/** 由 --init 显式调用，使用本机配置初始化实验数据。 */
final class RelationshipData {
    private RelationshipData() { }

    static void initialize() throws Exception {
        Properties settings = MyBatisUtil.getSqlSessionFactory().getConfiguration().getVariables();
        String url = settings.getProperty("db.url").replaceFirst("(jdbc:mysql://[^/]+/)[^?]*", "$1");
        Class.forName(settings.getProperty("db.driver"));
        try (Connection connection = DriverManager.getConnection(url,
                settings.getProperty("db.username"), settings.getProperty("db.password"))) {
            ScriptRunner runner = new ScriptRunner(connection);
            runner.setStopOnError(true);
            runner.setLogWriter(null);
            for (String script : new String[]{"sql/dept_emp_init.sql", "sql/relationship_init.sql"}) {
                try (Reader input = Files.newBufferedReader(Path.of(script), StandardCharsets.UTF_8)) {
                    runner.runScript(input);
                }
            }
        }
    }
}
