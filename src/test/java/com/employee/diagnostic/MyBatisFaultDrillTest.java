package com.employee.diagnostic;

import com.employee.entity.Employee;
import com.employee.util.MyBatisUtil;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** 任务五：从正式 XML 生成内存中的故障与修复版本，在同一事务中观察真实结果。 */
@Tag("database")
class MyBatisFaultDrillTest {
    private static final Logger LOG = LoggerFactory.getLogger(MyBatisFaultDrillTest.class);
    private static final String ORIGINAL = "com.employee.mapper.EmployeeMapper";
    private static final String BROKEN = "com.employee.diagnostic.BrokenEmployeeMapper";
    private static final String REPAIRED = "com.employee.diagnostic.RepairedEmployeeMapper";

    @BeforeAll
    static void requireTransactionalTable() throws java.sql.SQLException {
        try (SqlSession session = MyBatisUtil.openSession();
             java.sql.Statement statement = session.getConnection().createStatement();
             java.sql.ResultSet result = statement.executeQuery(
                     "SELECT ENGINE FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'staff_info'")) {
            assertTrue(result.next(), "请先创建 staff_info 表");
            assertEquals("InnoDB", result.getString(1), "故障演练需要事务回滚");
        }
    }

    private String originalXml() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("com/employee/mapper/EmployeeMapper.xml")) {
            assertNotNull(input, "必须能读取正式映射文件");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void withFault(String brokenXml, Consumer<SqlSession> exercise) throws IOException {
        Configuration base = MyBatisUtil.getSqlSessionFactory().getConfiguration();
        Configuration isolated = new Configuration(base.getEnvironment());
        isolated.setMapUnderscoreToCamelCase(base.isMapUnderscoreToCamelCase());
        isolated.setLogImpl(Slf4jImpl.class);
        isolated.getTypeAliasRegistry().registerAlias("Employee", Employee.class);
        parse(isolated, brokenXml.replace(ORIGINAL, BROKEN), "task5-broken.xml");
        parse(isolated, originalXml().replace(ORIGINAL, REPAIRED), "task5-repaired.xml");
        try (SqlSession session = new SqlSessionFactoryBuilder().build(isolated).openSession()) {
            try {
                exercise.accept(session);
            } finally {
                session.rollback();
                LOG.info("演练事务已回滚，正式 XML 未修改");
            }
        }
    }

    private void parse(Configuration configuration, String xml, String resource) {
        new XMLMapperBuilder(new StringReader(xml), configuration, resource, configuration.getSqlFragments()).parse();
    }

    private Employee emp() {
        return new Employee("Task5故障演练", "drill" + UUID.randomUUID().toString().replace("-", ""),
                "男", new BigDecimal("9.90"), "测试岗位", LocalDate.of(2024, 1, 1), 1);
    }

    private Employee select(SqlSession session, String namespace, Integer id) {
        return session.selectOne(namespace + ".selectById", Collections.singletonMap("empId", id));
    }

    @Test
    void wrongParameter_记录根因并在修复后插入成功() throws IOException {
        String xml = originalXml();
        String broken = xml.replace("VALUES (#{name},", "VALUES (#{nAme},");
        assertNotEquals(xml, broken, "必须确实制造参数拼写错误");
        withFault(broken, session -> {
            Employee sample = emp();
            PersistenceException error = assertThrows(PersistenceException.class,
                    () -> session.insert(BROKEN + ".insert", sample));
            Throwable cause = error;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            assertInstanceOf(ReflectionException.class, cause);
            assertTrue(cause.getMessage().contains("nAme"));
            LOG.info("演练一 参数拼写错误，实际根因：{}: {}", cause.getClass().getSimpleName(), cause.getMessage());
            LOG.info("预期异常堆栈（演练捕获，不代表测试失败）", error);
            assertEquals(1, session.insert(REPAIRED + ".insert", sample));
            assertNotNull(sample.getEmpId());
            assertEquals(sample.getName(), select(session, REPAIRED, sample.getEmpId()).getName());
            LOG.info("演练一 修复为 name 后，插入成功且姓名映射正确");
        });
    }

    @Test
    void wrongColumn_观察姓名为null并核对正确映射() throws IOException {
        String xml = originalXml();
        String broken = xml.replace("property=\"name\" column=\"full_name\"", "property=\"name\" column=\"missing_name\"");
        assertNotEquals(xml, broken, "必须确实制造列名错误");
        withFault(broken, session -> {
            Employee sample = emp();
            assertEquals(1, session.insert(REPAIRED + ".insert", sample));
            Employee wrong = select(session, BROKEN, sample.getEmpId());
            assertNotNull(wrong);
            assertNull(wrong.getName());
            assertEquals(sample.getDepartment(), wrong.getDepartment());
            LOG.info("演练二 SQL返回一行但错误列 missing_name 导致 name={}，其他字段仍有值", wrong.getName());
            Employee correct = select(session, REPAIRED, sample.getEmpId());
            assertEquals(sample.getName(), correct.getName());
            LOG.info("演练二 修复为真实列 full_name 后，姓名={}", correct.getName());
        });
    }

    @Test
    void missingGeneratedKeys_插入成功但主键为null修复后回填() throws IOException {
        String xml = originalXml();
        String broken = xml.replace(" useGeneratedKeys=\"true\"", "");
        assertNotEquals(xml, broken, "必须确实删除主键回填配置");
        withFault(broken, session -> {
            Employee sample = emp();
            assertEquals(1, session.insert(BROKEN + ".insert", sample));
            assertNull(sample.getEmpId());
            Employee condition = new Employee();
            condition.setDepartment(sample.getDepartment());
            java.util.List<Employee> stored = session.selectList(REPAIRED + ".selectByCondition", condition);
            assertEquals(1, stored.size());
            assertNotNull(stored.get(0).getEmpId(), "数据库实际生成了主键");
            LOG.info("演练三 INSERT影响一行，对象empId={}，数据库中主键={}；插入成功不等于回填成功",
                    sample.getEmpId(), stored.get(0).getEmpId());
            Employee repaired = emp();
            assertEquals(1, session.insert(REPAIRED + ".insert", repaired));
            assertNotNull(repaired.getEmpId());
            LOG.info("演练三 恢复 useGeneratedKeys 与 keyProperty 后，对象empId={}", repaired.getEmpId());
        });
    }
}
