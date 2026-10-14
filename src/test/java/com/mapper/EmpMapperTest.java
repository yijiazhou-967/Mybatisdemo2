package com.mapper;

import com.entity.Dept;
import com.entity.Emp;
import com.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

@Tag("database")
class EmpMapperTest extends MapperTestSupport {
    @Test
    void employeeCrudMapsFieldsAndLogicalDeletion() throws Exception {
        Dept dept = insertDept();
        Emp emp = insertEmp(dept.getDeptno(), "A");
        Emp loaded = employees.selectById(emp.getEmpno());
        assertEquals(emp.getEname(), loaded.getEname());
        assertEquals(emp.getJob(), loaded.getJob());
        assertEquals(emp.getHiredate().getTime(), loaded.getHiredate().getTime());
        assertEquals(emp.getSal(), loaded.getSal());
        assertNull(loaded.getComm());
        assertNull(loaded.getMgr());
        assertEquals(dept.getDeptno(), loaded.getDeptno());
        assertEquals(0, loaded.getDeleted());
        assertTrue(employees.selectAll().stream().anyMatch(item -> item.getEmpno().equals(emp.getEmpno())));
        emp.setJob("ANALYST");
        emp.setSal(8800.00);
        emp.setComm(0.00);
        emp.setHiredate(null);
        assertEquals(1, employees.update(emp));
        loaded = employees.selectById(emp.getEmpno());
        assertEquals("ANALYST", loaded.getJob());
        assertEquals(8800.00, loaded.getSal());
        assertEquals(0.00, loaded.getComm());
        assertNull(loaded.getHiredate());
        assertEquals(1, employees.deleteById(emp.getEmpno()));
        assertNull(employees.selectById(emp.getEmpno()));
        assertTrue(employees.selectByDeptno(dept.getDeptno()).isEmpty());
        assertEquals(0, employees.deleteById(emp.getEmpno()));
        assertEquals(0, employees.update(emp));
        try (PreparedStatement statement = session.getConnection().prepareStatement("SELECT deleted FROM emp WHERE empno = ?")) {
            statement.setInt(1, emp.getEmpno());
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
        }
    }

    @Test
    void employeeLoadsItsDepartmentAndDepartmentFilterWorks() {
        Dept dept = insertDept();
        Emp emp = insertEmp(dept.getDeptno(), "A");
        Emp loaded = employees.selectByIdWithDept(emp.getEmpno());
        assertEquals(emp.getEname(), loaded.getEname());
        assertNotNull(loaded.getDept());
        assertEquals(dept.getDeptno(), loaded.getDept().getDeptno());
        assertEquals(marker, loaded.getDept().getDname());
        assertEquals("杭州", loaded.getDept().getLoc());
        assertEquals(1, employees.selectByDeptno(dept.getDeptno()).size());
        assertEquals(emp.getEmpno(), employees.selectByDeptno(dept.getDeptno()).get(0).getEmpno());
    }

    @Test
    void employeeWithoutDepartmentHasNullAssociation() {
        Emp emp = insertEmp(null, "A");
        Emp loaded = employees.selectByIdWithDept(emp.getEmpno());
        assertNotNull(loaded);
        assertNull(loaded.getDeptno());
        assertNull(loaded.getDept());
    }

    @Test
    void deletedDepartmentIsNotExposedThroughAssociation() {
        Dept dept = insertDept();
        Emp emp = insertEmp(dept.getDeptno(), "A");
        departments.deleteById(dept.getDeptno());
        Emp loaded = employees.selectByIdWithDept(emp.getEmpno());
        assertNotNull(loaded);
        assertEquals(dept.getDeptno(), loaded.getDeptno());
        assertNull(loaded.getDept());
    }

    @Test
    void rollbackDoesNotSaveTemporaryRows() {
        Dept dept = insertDept();
        Emp emp = insertEmp(dept.getDeptno(), "A");
        session.rollback();
        try (SqlSession verification = MyBatisUtil.openSession()) {
            assertNull(verification.getMapper(EmpMapper.class).selectById(emp.getEmpno()));
            assertNull(verification.getMapper(DeptMapper.class).selectById(dept.getDeptno()));
        }
    }
}
