package com.employee.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.employee.entity.Employee;
import com.employee.query.EmployeeQueries;
import com.employee.query.EmployeeSearch;
import com.employee.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** 任务四真实 MySQL 验收：每个用例独立插入数据，最终回滚。 */
@Tag("database")
class EmployeePlusMapperTest {
    private SqlSession session;
    private EmployeePlusMapper mapper;
    private String marker;

    @BeforeAll
    static void requireTransactionalTable() throws java.sql.SQLException {
        try (SqlSession session = MyBatisUtil.openSession();
             java.sql.Statement statement = session.getConnection().createStatement();
             java.sql.ResultSet result = statement.executeQuery(
                     "SELECT ENGINE FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'staff_info'")) {
            assertTrue(result.next(), "请先创建 staff_info 表");
            assertEquals("InnoDB", result.getString(1), "测试需要事务型表");
        }
    }

    @BeforeEach
    void openSession() {
        session = MyBatisUtil.openSession();
        mapper = session.getMapper(EmployeePlusMapper.class);
        marker = "mp" + UUID.randomUUID().toString().replace("-", "");
    }

    @AfterEach
    void rollbackAndClose() {
        if (session != null) {
            try {
                session.rollback();
            } finally {
                session.close();
            }
        }
    }

    private Employee insert(String suffix, String salary, String gender, int status) {
        Employee emp = new Employee(marker + suffix, marker, gender, new BigDecimal(salary),
                "测试岗位", LocalDate.of(2024, 1, 1), status);
        emp.setEmail(UUID.randomUUID().toString() + "@company.example");
        emp.setOfficeLocation("杭州研发中心");
        assertEquals(1, mapper.insert(emp));
        assertNotNull(emp.getEmpId());
        return emp;
    }

    private List<Integer> ids(List<Employee> employees) {
        return employees.stream().map(Employee::getEmpId).collect(Collectors.toList());
    }

    private LambdaQueryWrapper<Employee> ownRecords() {
        return EmployeeQueries.lambdaQueryWrapper(null, null, null).eq(Employee::getDepartment, marker);
    }

    @Test
    void searchRangesMatchAcrossXmlAndBothWrappers() {
        Employee lower = insert("下界", "30.00", "男", 0);
        Employee upper = insert("上界", "40.00", "男", 0);
        insert("低于下界", "29.99", "男", 0);
        insert("在职", "35.00", "男", 1);
        Employee wrongOffice = insert("不同地点", "35.00", "男", 0);
        wrongOffice.setOfficeLocation("成都办公室");
        mapper.updateById(wrongOffice);
        Employee wrongDate = insert("不同日期", "35.00", "男", 0);
        wrongDate.setHireDate(LocalDate.of(2023, 12, 31));
        mapper.updateById(wrongDate);
        EmployeeSearch q = new EmployeeSearch().name(marker).department(marker).gender("男").status(0)
                .office("杭州研发中心").salaryBetween(new BigDecimal("30"), new BigDecimal("40"))
                .hiredBetween(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1));
        List<Integer> expected = Arrays.asList(upper.getEmpId(), lower.getEmpId());
        EmployeeMapper xml = session.getMapper(EmployeeMapper.class);
        assertEquals(expected, ids(xml.search(q)));
        assertEquals(expected, ids(xml.searchWithTrim(q)));
        assertEquals(expected, ids(mapper.selectList(EmployeeQueries.queryWrapper(q))));
        assertEquals(expected, ids(mapper.selectList(EmployeeQueries.lambdaQueryWrapper(q))));
        Page<Employee> page = mapper.findPage(q, 2, 1);
        assertEquals(2, page.getTotal());
        assertEquals(2, page.getPages());
        assertEquals(Arrays.asList(lower.getEmpId()), ids(page.getRecords()));
        assertTrue(mapper.findPage(q, 3, 1).getRecords().isEmpty());
    }

    @Test
    void departmentUsesExactMatchAndNameRemainsFuzzy() {
        Employee original = insert("姓名片段", "30.00", "男", 1);
        EmployeeMapper xml = session.getMapper(EmployeeMapper.class);
        Employee condition = new Employee();
        condition.setName("姓名片段");
        condition.setDepartment(marker.substring(2));
        assertTrue(xml.selectByCondition(condition).isEmpty());
        condition.setDepartment(marker);
        assertEquals(Arrays.asList(original.getEmpId()), ids(xml.selectByCondition(condition)));
        assertEquals(ids(xml.selectByCondition(condition)), ids(xml.selectByConditionWithTrim(condition)));
    }

    @Test
    void baseMapper_无XML完成CRUD与字段映射() {
        Employee original = insert("通用CRUD", "19.90", "男", 1);
        Employee loaded = mapper.selectById(original.getEmpId());
        assertEquals(original.getName(), loaded.getName());
        assertEquals(original.getDepartment(), loaded.getDepartment());
        assertEquals(original.getGender(), loaded.getGender());
        assertEquals(original.getSalary(), loaded.getSalary());
        assertEquals(original.getPost(), loaded.getPost());
        assertEquals(original.getEmail(), loaded.getEmail());
        assertEquals(original.getOfficeLocation(), loaded.getOfficeLocation());
        assertEquals(original.getHireDate(), loaded.getHireDate());
        assertEquals(original.getStatus(), loaded.getStatus());
        Employee patch = new Employee();
        patch.setEmpId(original.getEmpId());
        patch.setSalary(new BigDecimal("29.90"));
        patch.setPost("新岗位");
        patch.setEmail(UUID.randomUUID().toString() + "@company.example");
        patch.setOfficeLocation("成都办公室");
        patch.setStatus(0);
        assertEquals(1, mapper.updateById(patch));
        loaded = mapper.selectById(original.getEmpId());
        assertEquals(new BigDecimal("29.90"), loaded.getSalary());
        assertEquals("新岗位", loaded.getPost());
        assertEquals(patch.getEmail(), loaded.getEmail());
        assertEquals("成都办公室", loaded.getOfficeLocation());
        assertEquals(0, loaded.getStatus());
        assertEquals(original.getName(), loaded.getName());
        assertEquals(1, mapper.deleteById(original.getEmpId()));
        assertNull(mapper.selectById(original.getEmpId()));
    }

    @Test
    void wrappers_多条件与范围查询两种写法等效且排序稳定() {
        Employee lower = insert("匹配A", "30.00", "男", 1);
        Employee higher = insert("匹配B", "40.00", "男", 1);
        Employee tied = insert("匹配C", "40.00", "男", 1);
        insert("匹配低薪", "29.99", "男", 1);
        insert("匹配女", "99.00", "女", 1);
        insert("匹配离职", "99.00", "男", 0);
        insert("其他名称", "99.00", "男", 1);
        BigDecimal minimum = new BigDecimal("30.00");
        List<Integer> stringResult = ids(mapper.selectList(EmployeeQueries.queryWrapper(marker + "匹配", "男", minimum)));
        List<Integer> lambdaResult = ids(mapper.selectList(EmployeeQueries.lambdaQueryWrapper(marker + "匹配", "男", minimum)));
        assertEquals(Arrays.asList(higher.getEmpId(), tied.getEmpId(), lower.getEmpId()), stringResult);
        assertEquals(stringResult, lambdaResult);
    }

    @Test
    void wrappers_空白条件不参与筛选但仍排除离职记录() {
        Employee active = insert("空白条件", "0.00", "男", 1);
        Employee inactive = insert("离职", "99.00", "男", 0);
        List<Employee> found = mapper.selectList(EmployeeQueries.lambdaQueryWrapper(" ", "\t", null)
                .eq(Employee::getDepartment, marker));
        assertEquals(Arrays.asList(active.getEmpId()), ids(found));
        assertNotNull(mapper.selectById(inactive.getEmpId()), "离职状态不是逻辑删除");
    }

    @Test
    void pagination_首页中间页末页条数总数页数和偏移正确() {
        for (int index = 0; index < 7; index++) {
            insert("分页" + index, "10.00", "男", 1);
        }
        insert("分页离职", "10.00", "男", 0);
        List<Integer> all = ids(mapper.selectList(ownRecords()));
        for (int current = 1; current <= 3; current++) {
            Page<Employee> page = mapper.selectPage(new Page<>(current, 3), ownRecords());
            assertEquals(7, page.getTotal());
            assertEquals(3, page.getPages());
            assertEquals(current, page.getCurrent());
            assertEquals(3, page.getSize());
            int start = (current - 1) * 3;
            assertEquals(all.subList(start, Math.min(start + 3, all.size())), ids(page.getRecords()));
        }
    }

    @Test
    void pagination_超过总页数返回空记录而不重置页码() {
        insert("越界页", "10.00", "男", 1);
        Page<Employee> page = mapper.selectPage(new Page<>(99, 3), ownRecords());
        assertEquals(1, page.getTotal());
        assertEquals(1, page.getPages());
        assertEquals(99, page.getCurrent());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void pagination_无匹配数据返回零记录零页数() {
        Page<Employee> page = mapper.selectPage(new Page<>(1, 3), ownRecords());
        assertEquals(0, page.getTotal());
        assertEquals(0, page.getPages());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void coexist_原XML和通用Mapper在同一事务读取一致且可回滚() {
        Employee original = insert("共存", "19.90", "男", 1);
        EmployeeMapper legacy = session.getMapper(EmployeeMapper.class);
        assertEquals(original.getName(), legacy.selectById(original.getEmpId()).getName());
        original.setSalary(new BigDecimal("39.90"));
        assertEquals(1, legacy.update(original));
        assertEquals(new BigDecimal("39.90"), mapper.selectById(original.getEmpId()).getSalary());
        session.rollback();
        try (SqlSession verification = MyBatisUtil.openSession()) {
            assertNull(verification.getMapper(EmployeePlusMapper.class).selectById(original.getEmpId()));
        }
    }
}
