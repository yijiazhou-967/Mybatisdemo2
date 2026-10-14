package com.mapper;

import com.entity.Dept;
import com.entity.Emp;
import com.util.MyBatisUtil;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/** 默认即可运行：检查实际 XML、实体别名、参数绑定与日志配置，不连接数据库。 */
class MapperConfigurationTest {
    private final Configuration configuration = MyBatisUtil.getSqlSessionFactory().getConfiguration();

    @Test
    void aliasesAndMapperStatementsAreRegistered() {
        assertEquals(Dept.class, configuration.getTypeAliasRegistry().resolveAlias("Dept"));
        assertEquals(Emp.class, configuration.getTypeAliasRegistry().resolveAlias("Emp"));
        for (Class<?> mapper : Arrays.asList(DeptMapper.class, EmpMapper.class)) {
            Arrays.stream(mapper.getMethods()).forEach(method -> assertTrue(
                    configuration.hasStatement(mapper.getName() + "." + method.getName())));
        }
        assertEquals(Log4jImpl.class, configuration.getLogImpl());
        assertNotNull(getClass().getClassLoader().getResource("log4j.properties"));
    }

    @Test
    void relationshipsUseJoinedResultMaps() {
        var collection = configuration.getResultMap("com.mapper.DeptMapper.deptWithEmpsMap")
                .getResultMappings().stream().filter(mapping -> "emps".equals(mapping.getProperty())).findFirst().orElseThrow();
        assertEquals("com.mapper.EmpMapper.empMap", collection.getNestedResultMapId());
        assertEquals("e_", collection.getColumnPrefix());
        var association = configuration.getResultMap("com.mapper.EmpMapper.empWithDeptMap")
                .getResultMappings().stream().filter(mapping -> "dept".equals(mapping.getProperty())).findFirst().orElseThrow();
        assertEquals("com.mapper.DeptMapper.deptMap", association.getNestedResultMapId());
        assertEquals("d_", association.getColumnPrefix());
    }

    @Test
    void idsAreBoundAndDeletesAreLogical() {
        BoundSql select = configuration.getMappedStatement("com.mapper.EmpMapper.selectById")
                .getBoundSql(Collections.singletonMap("empno", 1));
        assertTrue(select.getSql().contains("empno = ?"));
        assertEquals("empno", select.getParameterMappings().get(0).getProperty());
        for (String mapper : Arrays.asList("DeptMapper", "EmpMapper")) {
            String sql = configuration.getMappedStatement("com.mapper." + mapper + ".deleteById")
                    .getBoundSql(Collections.emptyMap()).getSql();
            assertTrue(sql.contains("SET deleted = 1"));
            assertTrue(sql.contains("AND deleted = 0"));
        }
    }
}
