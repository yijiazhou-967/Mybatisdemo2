package com.mapper;

import com.entity.Dept;
import com.entity.Emp;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Tag("database")
class DeptMapperTest extends MapperTestSupport {
    @Test
    void departmentCrudAndLogicalDelete() throws Exception {
        Dept dept = insertDept();
        Dept loaded = departments.selectById(dept.getDeptno());
        assertEquals(marker, loaded.getDname());
        assertEquals("杭州", loaded.getLoc());
        assertEquals(0, loaded.getDeleted());
        assertTrue(departments.selectAll().stream().anyMatch(item -> item.getDeptno().equals(dept.getDeptno())));
        dept.setLoc("上海");
        assertEquals(1, departments.update(dept));
        assertEquals("上海", departments.selectById(dept.getDeptno()).getLoc());
        assertEquals(1, departments.deleteById(dept.getDeptno()));
        assertNull(departments.selectById(dept.getDeptno()));
        assertEquals(0, departments.deleteById(dept.getDeptno()));
        assertEquals(0, departments.update(dept));
        try (PreparedStatement statement = session.getConnection().prepareStatement("SELECT deleted FROM dept WHERE deptno = ?")) {
            statement.setInt(1, dept.getDeptno());
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
        }
    }

    @Test
    void oneDepartmentLoadsMultipleEmployeesAndExcludesDeletedOnes() {
        Dept dept = insertDept();
        Emp first = insertEmp(dept.getDeptno(), "A");
        Emp second = insertEmp(dept.getDeptno(), "B");
        Emp removed = insertEmp(dept.getDeptno(), "C");
        assertEquals(1, employees.deleteById(removed.getEmpno()));
        Dept loaded = departments.selectByIdWithEmps(dept.getDeptno());
        assertEquals(marker, loaded.getDname());
        assertEquals(Arrays.asList(first.getEmpno(), second.getEmpno()), loaded.getEmps().stream()
                .map(Emp::getEmpno).collect(Collectors.toList()));
        assertEquals(first.getEname(), loaded.getEmps().get(0).getEname());
        assertEquals(6500.00, loaded.getEmps().get(0).getSal());
        assertEquals(dept.getDeptno(), loaded.getEmps().get(0).getDeptno());
    }

    @Test
    void emptyDepartmentHasEmptyCollection() {
        Dept dept = insertDept();
        Dept loaded = departments.selectByIdWithEmps(dept.getDeptno());
        assertNotNull(loaded);
        assertNotNull(loaded.getEmps());
        assertTrue(loaded.getEmps().isEmpty());
    }

    @Test
    void deletedDepartmentIsAbsentFromJoinedQuery() {
        Dept dept = insertDept();
        insertEmp(dept.getDeptno(), "A");
        assertEquals(1, departments.deleteById(dept.getDeptno()));
        assertNull(departments.selectByIdWithEmps(dept.getDeptno()));
    }
}
