package com.employee.mapper;

import com.employee.entity.Employee;
import com.employee.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** 真实 MySQL 测试：独立创建数据，整个用例使用一个事务，结束时回滚。 */
@Tag("database")
class EmployeeMapperTest {
    private SqlSession session;
    private EmployeeMapper mapper;
    private String marker;

    @BeforeAll
    static void requireTransactionalTable() throws java.sql.SQLException {
        // 回滚测试要求事务型表；先核实引擎，避免在非事务表中留下测试数据。
        try (SqlSession session = MyBatisUtil.openSession();
             java.sql.Statement statement = session.getConnection().createStatement();
             java.sql.ResultSet result = statement.executeQuery(
                     "SELECT ENGINE FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'staff_info'")) {
            assertTrue(result.next(), "请先执行 sql/init.sql 创建 staff_info 表");
            assertEquals("InnoDB", result.getString(1), "数据库测试需要 InnoDB 表以保证事务回滚");
        }
    }

    @BeforeEach
    void openSession() {
        session = MyBatisUtil.openSession();
        mapper = session.getMapper(EmployeeMapper.class);
        marker = "test" + UUID.randomUUID().toString().replace("-", "");
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

    private Employee emp(String suffix, int status) {
        Employee employee = new Employee(marker + suffix, marker, "男",
                new BigDecimal("9.90"), "测试岗位", LocalDate.of(2024, 1, 1), status);
        employee.setEmail(UUID.randomUUID().toString() + "@company.example");
        employee.setOfficeLocation("杭州研发中心");
        return employee;
    }

    private Employee insert(String suffix, int status) {
        Employee emp = emp(suffix, status);
        assertEquals(1, mapper.insert(emp));
        assertNotNull(emp.getEmpId());
        return emp;
    }

    private List<Integer> ids(List<Employee> employees) {
        return employees.stream().map(Employee::getEmpId).collect(Collectors.toList());
    }

    @Test
    void insert_回填主键并映射全部字段() {
        Employee original = insert("插入", 0);
        Employee loaded = mapper.selectById(original.getEmpId());
        assertNotNull(loaded);
        assertEquals(original.getEmpId(), loaded.getEmpId());
        assertEquals(original.getName(), loaded.getName());
        assertEquals(original.getDepartment(), loaded.getDepartment());
        assertEquals(original.getGender(), loaded.getGender());
        assertEquals(original.getSalary(), loaded.getSalary());
        assertEquals(original.getPost(), loaded.getPost());
        assertEquals(original.getHireDate(), loaded.getHireDate());
        assertEquals(original.getEmail(), loaded.getEmail());
        assertEquals(original.getOfficeLocation(), loaded.getOfficeLocation());
        assertEquals(0, loaded.getStatus());
    }

    @Test
    void insert_入职日期允许为空() {
        Employee original = emp("无日期", 1);
        original.setHireDate(null);
        assertEquals(1, mapper.insert(original));
        assertNull(mapper.selectById(original.getEmpId()).getHireDate());
    }

    @Test
    void selectAll_包含自建数据且主键升序() {
        Employee a = insert("查询A", 1);
        Employee b = insert("查询B", 0);
        List<Integer> result = ids(mapper.selectAll());
        assertTrue(result.containsAll(Arrays.asList(a.getEmpId(), b.getEmpId())));
        assertEquals(result.stream().sorted().collect(Collectors.toList()), result);
    }

    @Test
    void update_整体更新并可将入职日期置空() {
        Employee original = insert("更新前", 1);
        original.setName(marker + "更新后");
        original.setDepartment(marker + "新部门");
        original.setGender("女");
        original.setSalary(new BigDecimal("88.80"));
        original.setPost("新岗位");
        original.setEmail(null);
        original.setOfficeLocation("上海办公室");
        original.setHireDate(null);
        original.setStatus(0);
        assertEquals(1, mapper.update(original));
        Employee loaded = mapper.selectById(original.getEmpId());
        assertEquals(original.getName(), loaded.getName());
        assertEquals(original.getDepartment(), loaded.getDepartment());
        assertEquals("女", loaded.getGender());
        assertEquals(new BigDecimal("88.80"), loaded.getSalary());
        assertEquals("新岗位", loaded.getPost());
        assertNull(loaded.getEmail());
        assertEquals("上海办公室", loaded.getOfficeLocation());
        assertEquals(0, loaded.getStatus());
        assertNull(loaded.getHireDate());
    }

    @Test
    void deleteById_删除后查询为空再次删除返回零() {
        Employee original = insert("删除", 1);
        assertEquals(1, mapper.deleteById(original.getEmpId()));
        assertNull(mapper.selectById(original.getEmpId()));
        assertEquals(0, mapper.deleteById(original.getEmpId()));
    }

    @Test
    void selectByCondition_条件叠加支持零状态与模糊查询() {
        Employee match = insert("匹配", 0);
        insert("不匹配状态", 1);
        Employee other = emp("不匹配性别", 0);
        other.setGender("女");
        mapper.insert(other);
        Employee condition = new Employee();
        condition.setName(marker);
        condition.setDepartment(marker.substring(4));
        condition.setGender("男");
        condition.setStatus(0);
        assertEquals(Arrays.asList(match.getEmpId()), ids(mapper.selectByCondition(condition)));
        assertEquals(ids(mapper.selectByCondition(condition)), ids(mapper.selectByConditionWithTrim(condition)));
    }

    @Test
    void selectByCondition_空条件与trim写法均返回全部() {
        insert("空条件", 1);
        List<Integer> all = ids(mapper.selectAll());
        assertEquals(all, ids(mapper.selectByCondition(new Employee())));
        assertEquals(all, ids(mapper.selectByCondition(null)));
        assertEquals(all, ids(mapper.selectByConditionWithTrim(new Employee())));
    }

    @Test
    void updateSelective_仅更新非空字段并支持零值() {
        Employee original = insert("局部更新", 1);
        Employee patch = new Employee();
        patch.setEmpId(original.getEmpId());
        patch.setSalary(new BigDecimal("66.60"));
        patch.setPost("新岗位");
        patch.setOfficeLocation("成都办公室");
        patch.setStatus(0);
        assertEquals(1, mapper.updateSelective(patch));
        Employee loaded = mapper.selectById(original.getEmpId());
        assertEquals(new BigDecimal("66.60"), loaded.getSalary());
        assertEquals("新岗位", loaded.getPost());
        assertEquals("成都办公室", loaded.getOfficeLocation());
        assertEquals(original.getEmail(), loaded.getEmail());
        assertEquals(0, loaded.getStatus());
        assertEquals(original.getName(), loaded.getName());
        assertEquals(original.getDepartment(), loaded.getDepartment());
        assertEquals(original.getHireDate(), loaded.getHireDate());
    }

    @Test
    void insertBatch_批量插入删除只影响本用例记录() {
        Employee survivor = insert("保留", 1);
        List<Employee> batch = Arrays.asList(emp("批量A", 1), emp("批量B", 0), emp("批量C", 1));
        assertEquals(3, mapper.insertBatch(batch));
        Employee condition = new Employee();
        condition.setName(marker + "批量");
        List<Integer> inserted = ids(mapper.selectByCondition(condition));
        assertEquals(3, inserted.size());
        assertEquals(3, mapper.deleteBatch(inserted));
        for (Integer id : inserted) {
            assertNull(mapper.selectById(id));
        }
        assertNotNull(mapper.selectById(survivor.getEmpId()));
    }

    @Test
    void selectSmart_姓名优先其次部门无条件查全部() {
        Employee first = insert("分支A", 1);
        Employee second = insert("分支B", 0);
        Employee name = new Employee();
        name.setName(first.getName());
        name.setDepartment("不会匹配的部门");
        assertEquals(Arrays.asList(first.getEmpId()), ids(mapper.selectSmart(name)));
        Employee department = new Employee();
        department.setDepartment(marker);
        assertEquals(Arrays.asList(first.getEmpId(), second.getEmpId()), ids(mapper.selectSmart(department)));
        assertEquals(ids(mapper.selectAll()), ids(mapper.selectSmart(new Employee())));
        assertEquals(ids(mapper.selectAll()), ids(mapper.selectSmart(null)));
    }

    @Test
    void rollback_插入记录不会被持久保存() {
        Employee original = insert("回滚", 1);
        session.rollback();
        try (SqlSession verification = MyBatisUtil.openSession()) {
            assertNull(verification.getMapper(EmployeeMapper.class).selectById(original.getEmpId()));
        }
    }
}
