package com.employee.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.employee.entity.Employee;
import com.employee.query.EmployeeQueries;
import com.employee.util.MyBatisUtil;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** 无数据库测试：确认 MP SQL 注入、实体注解与真正生效的分页配置。 */
class EmployeePlusMapperConfigTest {
    private final Configuration configuration = MyBatisUtil.getSqlSessionFactory().getConfiguration();

    @Test
    void configuration_通用SQL与原XML映射共存() {
        assertTrue(configuration instanceof MybatisConfiguration);
        for (String method : Arrays.asList("insert", "selectById", "updateById", "deleteById", "selectList", "selectCount")) {
            assertTrue(configuration.hasStatement("com.employee.mapper.EmployeePlusMapper." + method), method);
        }
        assertTrue(configuration.hasStatement("com.employee.mapper.EmployeeMapper.updateSelectiveRows"));
        assertTrue(configuration.hasMapper(EmployeePlusMapper.class));
    }

    @Test
    void pagination_已注册MySQL分页且保持页码溢出为空() {
        assertEquals(1, configuration.getInterceptors().size());
        MybatisPlusInterceptor outer = (MybatisPlusInterceptor) configuration.getInterceptors().get(0);
        assertEquals(1, outer.getInterceptors().size());
        PaginationInnerInterceptor pagination = (PaginationInnerInterceptor) outer.getInterceptors().get(0);
        assertEquals(DbType.MYSQL, pagination.getDbType());
        assertFalse(pagination.isOverflow());
        assertEquals(Long.valueOf(100), pagination.getMaxLimit());
    }

    @Test
    void entity_自增主键日期映射与状态含义正确() {
        TableInfo table = TableInfoHelper.getTableInfo(Employee.class);
        assertNotNull(table);
        assertEquals("staff_info", table.getTableName());
        assertEquals("staff_id", table.getKeyColumn());
        assertEquals(IdType.AUTO, table.getIdType());
        assertFalse(table.isWithLogicDelete());
        assertTrue(table.getFieldList().stream().anyMatch(field ->
                "hireDate".equals(field.getProperty()) && "entry_date".equals(field.getColumn())));
    }

    @Test
    void wrappers_空白条件被忽略且输入使用绑定参数() {
        String blank = EmployeeQueries.queryWrapper(" ", "\t", null).getSqlSegment();
        assertFalse(blank.contains("full_name LIKE"));
        assertFalse(blank.contains("gender ="));
        assertFalse(blank.contains("monthly_salary >="));
        assertTrue(blank.contains("work_status ="));
        assertTrue(blank.contains("ORDER BY monthly_salary DESC,staff_id ASC"));
        String input = "' OR 1=1 --";
        var query = EmployeeQueries.queryWrapper(input, "男", new BigDecimal("30.00"));
        var lambda = EmployeeQueries.lambdaQueryWrapper(input, "男", new BigDecimal("30.00"));
        assertEquals(query.getSqlSegment(), lambda.getSqlSegment());
        assertFalse(query.getSqlSegment().contains(input));
        assertEquals(query.getParamNameValuePairs(), lambda.getParamNameValuePairs());
    }
}
