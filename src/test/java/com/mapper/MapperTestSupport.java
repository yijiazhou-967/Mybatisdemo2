package com.mapper;

import com.entity.Dept;
import com.entity.Emp;
import com.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 每个测试使用独立事务和自建记录，结束时回滚，不依赖固定样例编号。 */
abstract class MapperTestSupport {
    protected SqlSession session;
    protected DeptMapper departments;
    protected EmpMapper employees;
    protected String marker;

    @BeforeAll
    static void requireTransactionalTables() throws Exception {
        try (SqlSession session = MyBatisUtil.openSession();
             Statement statement = session.getConnection().createStatement();
             ResultSet rows = statement.executeQuery("SELECT table_name, engine FROM information_schema.tables "
                     + "WHERE table_schema = DATABASE() AND table_name IN ('dept', 'emp')")) {
            int count = 0;
            while (rows.next()) {
                assertEquals("InnoDB", rows.getString("engine"));
                count++;
            }
            assertEquals(2, count, "请先执行 sql/dept_emp_init.sql");
        }
    }

    @BeforeEach
    void openSession() {
        session = MyBatisUtil.openSession();
        departments = session.getMapper(DeptMapper.class);
        employees = session.getMapper(EmpMapper.class);
        marker = "lab" + UUID.randomUUID().toString().replace("-", "");
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

    protected Dept insertDept() {
        Dept dept = new Dept(null, marker, "杭州");
        assertEquals(1, departments.insert(dept));
        assertNotNull(dept.getDeptno());
        return dept;
    }

    protected Emp insertEmp(Integer deptno, String suffix) {
        Emp emp = new Emp(null, marker + suffix, "DEVELOPER", null,
                Date.valueOf("2025-03-12"), 6500.00, null, deptno);
        assertEquals(1, employees.insert(emp));
        assertNotNull(emp.getEmpno());
        return emp;
    }
}
