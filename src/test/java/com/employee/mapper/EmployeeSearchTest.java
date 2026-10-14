package com.employee.mapper;

import com.employee.query.EmployeeQueries;
import com.employee.query.EmployeeSearch;
import com.employee.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeSearchTest {
    @Test
    void blankInputsAndZeroValuesHaveDistinctMeanings() {
        EmployeeSearch q = new EmployeeSearch().name("  ").department(" 技术部 ")
                .gender("\t").office(" ").status(0).salaryBetween(BigDecimal.ZERO, null).validate();
        assertNull(q.getName());
        assertNull(q.getGender());
        assertNull(q.getOfficeLocation());
        assertEquals("技术部", q.getDepartment());
        assertEquals(0, q.getStatus());
        assertEquals(BigDecimal.ZERO, q.getMinimumSalary());
    }

    @Test
    void invalidRangesAndEnumsAreRejectedBeforeConnecting() {
        assertThrows(IllegalArgumentException.class, () -> new EmployeeSearch().status(2).validate());
        assertThrows(IllegalArgumentException.class, () -> new EmployeeSearch().gender("unknown").validate());
        assertThrows(IllegalArgumentException.class, () -> new EmployeeSearch()
                .salaryBetween(new BigDecimal("-1"), null).validate());
        assertThrows(IllegalArgumentException.class, () -> EmployeeQueries.queryWrapper(new EmployeeSearch()
                .salaryBetween(new BigDecimal("20"), new BigDecimal("10"))));
        try (SqlSession session = MyBatisUtil.openSession()) {
            EmployeeSearch invalid = new EmployeeSearch()
                    .hiredBetween(LocalDate.of(2025, 1, 1), LocalDate.of(2024, 1, 1));
            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);
            assertThrows(IllegalArgumentException.class, () -> mapper.search(invalid));
            assertThrows(IllegalArgumentException.class, () -> mapper.searchWithTrim(invalid));
        }
    }

    @Test
    void wrappersBindAllNineConditionsWithoutEmbeddingInput() {
        MyBatisUtil.getSqlSessionFactory(); // 初始化 MP 实体元数据，供 Lambda 属性解析使用。
        EmployeeSearch q = new EmployeeSearch().department("技术部").status(0)
                .salaryBetween(BigDecimal.ZERO, new BigDecimal("15000"))
                .hiredBetween(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31))
                .office("杭州研发中心").gender("男").name("' OR 1=1 --");
        var strings = EmployeeQueries.queryWrapper(q);
        var lambda = EmployeeQueries.lambdaQueryWrapper(q);
        assertEquals(strings.getSqlSegment(), lambda.getSqlSegment());
        assertEquals(strings.getParamNameValuePairs(), lambda.getParamNameValuePairs());
        assertEquals(9, strings.getParamNameValuePairs().size());
        assertFalse(strings.getSqlSegment().contains(q.getName()));
    }

    @Test
    void xmlWhereAndTrimShareRangeAndDateBindings() {
        var config = MyBatisUtil.getSqlSessionFactory().getConfiguration();
        EmployeeSearch q = new EmployeeSearch().status(0).salaryBetween(BigDecimal.ZERO, new BigDecimal("1"))
                .hiredBetween(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1));
        var parameters = Collections.singletonMap("q", q);
        var where = config.getMappedStatement("com.employee.mapper.EmployeeMapper.searchRows").getBoundSql(parameters);
        var trim = config.getMappedStatement("com.employee.mapper.EmployeeMapper.searchTrimRows").getBoundSql(parameters);
        assertEquals(where.getSql().replaceAll("\\s+", " "), trim.getSql().replaceAll("\\s+", " "));
        assertEquals(5, where.getParameterMappings().size());
        assertTrue(where.getSql().contains("monthly_salary >="));
        assertTrue(where.getSql().contains("monthly_salary <="));
        assertTrue(where.getSql().contains("entry_date >="));
        assertTrue(where.getSql().contains("entry_date <="));
    }

    @Test
    void pageBoundsAreCheckedBeforeConnecting() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            EmployeePlusMapper mapper = session.getMapper(EmployeePlusMapper.class);
            assertThrows(IllegalArgumentException.class, () -> mapper.findPage(null, 0, 3));
            assertThrows(IllegalArgumentException.class, () -> mapper.findPage(null, 1, 0));
            assertThrows(IllegalArgumentException.class, () -> mapper.findPage(null, 1, 101));
        }
    }
}
