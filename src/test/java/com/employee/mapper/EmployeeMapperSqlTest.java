package com.employee.mapper;

import com.employee.entity.Employee;
import com.employee.util.MyBatisUtil;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/** 不连接数据库：验证实际 MyBatis 动态 SQL、绑定参数及 Mapper 参数保护。 */
class EmployeeMapperSqlTest {
    private final Configuration configuration = MyBatisUtil.getSqlSessionFactory().getConfiguration();

    private BoundSql bound(String method, Object parameter) {
        return configuration.getMappedStatement("com.employee.mapper.EmployeeMapper." + method).getBoundSql(parameter);
    }

    private String sql(String method, Object parameter) {
        return bound(method, parameter).getSql().replaceAll("\\s+", " ").trim();
    }

    @Test
    void configuration_每个抽象方法都有映射且结果字段齐全() {
        Arrays.stream(EmployeeMapper.class.getMethods()).filter(method -> !method.isDefault())
                .forEach(method -> assertTrue(configuration.hasStatement("com.employee.mapper.EmployeeMapper." + method.getName())));
        assertEquals(10, configuration.getResultMap("com.employee.mapper.EmployeeMapper.empMap").getResultMappings().size());
        assertTrue(configuration.isMapUnderscoreToCamelCase());
    }

    @Test
    void where_trim等效并正确绑定四个条件() {
        Employee condition = new Employee();
        condition.setName("' OR 1=1 --");
        condition.setDepartment("部门");
        condition.setGender("男");
        condition.setStatus(0);
        String where = sql("selectByCondition", condition);
        assertEquals(where, sql("selectByConditionWithTrim", condition));
        assertTrue(where.contains("WHERE full_name LIKE CONCAT('%', ?, '%') AND department LIKE CONCAT('%', ?, '%') AND gender = ? AND work_status = ?"));
        assertFalse(where.contains(condition.getName()));
        assertEquals(4, bound("selectByCondition", condition).getParameterMappings().size());
    }

    @Test
    void where_空条件不生成多余WHERE且零状态不被忽略() {
        Employee condition = new Employee();
        condition.setName("");
        assertFalse(sql("selectByCondition", condition).contains("WHERE"));
        assertEquals(sql("selectByCondition", null), sql("selectByConditionWithTrim", null));
        condition.setStatus(0);
        assertTrue(sql("selectByCondition", condition).contains("WHERE work_status = ?"));
    }

    @Test
    void choose_姓名优先否则部门无条件查全部() {
        Employee condition = new Employee();
        condition.setName("姓名");
        condition.setDepartment("部门");
        assertTrue(sql("selectSmart", condition).contains("WHERE full_name = ?"));
        assertFalse(sql("selectSmart", condition).contains("department = ?"));
        condition.setName("");
        assertTrue(sql("selectSmart", condition).contains("WHERE department = ?"));
        assertTrue(sql("selectSmart", null).contains("WHERE 1 = 1"));
        assertFalse(sql("selectSmart", new Employee()).contains("work_status ="));
    }

    @Test
    void set_仅生成有值字段且无末尾逗号() {
        Employee patch = new Employee();
        patch.setEmpId(1);
        patch.setSalary(java.math.BigDecimal.ZERO);
        patch.setStatus(0);
        assertEquals("UPDATE staff_info SET monthly_salary = ?, work_status = ? WHERE staff_id = ?", sql("updateSelectiveRows", patch));
        assertEquals(3, bound("updateSelectiveRows", patch).getParameterMappings().size());
    }

    @Test
    void foreach_生成正确占位符与绑定数量() {
        assertEquals("DELETE FROM staff_info WHERE staff_id IN ( ? , ? )",
                sql("deleteBatchRows", Collections.singletonMap("ids", Arrays.asList(1, 2))));
        BoundSql batch = bound("insertBatchRows", Collections.singletonMap("employees", Arrays.asList(new Employee(), new Employee())));
        assertEquals(18, batch.getParameterMappings().size());
        assertEquals(2, batch.getParameterMappings().stream().filter(p -> p.getProperty().endsWith(".hireDate")).count());
    }

    @Test
    void emptyBatch_返回零且无需建立数据库连接() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);
            assertEquals(0, mapper.insertBatch(Collections.emptyList()));
            assertEquals(0, mapper.deleteBatch(Collections.emptyList()));
        }
    }

    @Test
    void invalidBatch_在执行SQL之前拒绝无效参数() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);
            assertThrows(IllegalArgumentException.class, () -> mapper.insertBatch(null));
            assertThrows(IllegalArgumentException.class, () -> mapper.insertBatch(Arrays.asList(new Employee(), null)));
            assertThrows(IllegalArgumentException.class, () -> mapper.deleteBatch(null));
            assertThrows(IllegalArgumentException.class, () -> mapper.deleteBatch(Arrays.asList(1, null)));
            assertThrows(IllegalArgumentException.class, () -> mapper.deleteBatch(Arrays.asList(0)));
        }
    }

    @Test
    void invalidPatch_拒绝缺少主键或更新字段() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);
            assertThrows(IllegalArgumentException.class, () -> mapper.updateSelective(null));
            Employee patch = new Employee();
            patch.setSalary(java.math.BigDecimal.ZERO);
            assertThrows(IllegalArgumentException.class, () -> mapper.updateSelective(patch));
            patch.setEmpId(0);
            assertThrows(IllegalArgumentException.class, () -> mapper.updateSelective(patch));
            Employee empty = new Employee();
            empty.setEmpId(1);
            assertThrows(IllegalArgumentException.class, () -> mapper.updateSelective(empty));
        }
    }
}
