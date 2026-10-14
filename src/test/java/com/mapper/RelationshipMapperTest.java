package com.mapper;

import com.entity.Dept;
import com.entity.Emp;
import com.entity.Project;
import com.util.MyBatisUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Tag("database")
class RelationshipMapperTest extends MapperTestSupport {
    @BeforeAll
    static void requireRelationshipTables() throws Exception {
        try (var session = MyBatisUtil.openSession(); Statement statement = session.getConnection().createStatement();
             ResultSet rows = statement.executeQuery("SELECT engine FROM information_schema.tables "
                     + "WHERE table_schema = DATABASE() AND table_name IN ('project','employee_project','emp_card')")) {
            int count = 0;
            while (rows.next()) { assertEquals("InnoDB", rows.getString(1)); count++; }
            assertEquals(3, count, "请先运行 query.cmd --init");
        }
    }

    private Project project(String suffix) {
        Project project = new Project();
        project.setProjectCode(marker.substring(0, 20) + suffix);
        project.setTitle("关联测试" + suffix);
        project.setStartDate(LocalDate.of(2026, 3, 1));
        assertEquals(1, session.getMapper(ProjectMapper.class).insert(project));
        assertNotNull(project.getProjectId());
        return project;
    }

    private void execute(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
            session.clearCache();
        }
    }

    private void member(Project project, Emp employee) throws SQLException {
        execute("INSERT INTO employee_project (project_id, empno) VALUES (?, ?)",
                project.getProjectId(), employee.getEmpno());
    }

    @Test
    void projectAndEmployeeBothLoadMultipleRelatedRows() throws Exception {
        Emp first = insertEmp(insertDept().getDeptno(), "A");
        Emp second = insertEmp(first.getDeptno(), "B");
        Project alpha = project("A");
        Project beta = project("B");
        member(alpha, first); member(alpha, second); member(beta, first);
        var loaded = session.getMapper(ProjectMapper.class).selectByIdWithEmps(alpha.getProjectId());
        assertEquals(Arrays.asList(first.getEmpno(), second.getEmpno()), loaded.getEmps().stream()
                .map(Emp::getEmpno).collect(Collectors.toList()));
        var participant = employees.selectByIdWithProjects(first.getEmpno());
        assertEquals(Arrays.asList(alpha.getProjectId(), beta.getProjectId()), participant.getProjects().stream()
                .map(Project::getProjectId).collect(Collectors.toList()));
        assertEquals(alpha.getStartDate(), participant.getProjects().get(0).getStartDate());
        assertEquals(2, loaded.getEmps().size());
    }

    @Test
    void emptyRelationshipsRemainEmptyAndMissingParentsRemainNull() {
        Project empty = project("empty");
        Emp unassigned = insertEmp(null, "no-project");
        assertTrue(session.getMapper(ProjectMapper.class).selectByIdWithEmps(empty.getProjectId()).getEmps().isEmpty());
        assertTrue(employees.selectByIdWithProjects(unassigned.getEmpno()).getProjects().isEmpty());
        assertNull(employees.selectByIdWithCard(unassigned.getEmpno()).getCard());
        assertNull(employees.selectByIdWithDept(unassigned.getEmpno()).getDept());
        assertNull(employees.selectByIdWithProjects(Integer.MAX_VALUE));
        assertNull(employees.selectByIdWithCard(Integer.MAX_VALUE));
        assertNull(session.getMapper(ProjectMapper.class).selectByIdWithEmps(Integer.MAX_VALUE));
    }

    @Test
    void deletedTargetsAreHiddenInBothProjectDirections() throws Exception {
        Emp employee = insertEmp(null, "deleted");
        Project project = project("deleted");
        member(project, employee);
        assertEquals(1, employees.deleteById(employee.getEmpno()));
        assertTrue(session.getMapper(ProjectMapper.class).selectByIdWithEmps(project.getProjectId()).getEmps().isEmpty());
        assertNull(employees.selectByIdWithProjects(employee.getEmpno()));
        Emp active = insertEmp(null, "active");
        member(project, active);
        execute("UPDATE project SET deleted = 1 WHERE project_id = ?", project.getProjectId());
        assertTrue(employees.selectByIdWithProjects(active.getEmpno()).getProjects().isEmpty());
        assertNull(session.getMapper(ProjectMapper.class).selectByIdWithEmps(project.getProjectId()));
    }

    @Test
    void cardIsOneToOneAndDatabaseRejectsSecondCard() throws Exception {
        Emp holder = insertEmp(null, "card");
        execute("INSERT INTO emp_card (empno, card_number, issued_on) VALUES (?, ?, ?)",
                holder.getEmpno(), marker, java.sql.Date.valueOf("2026-03-01"));
        var card = employees.selectByIdWithCard(holder.getEmpno()).getCard();
        assertEquals(holder.getEmpno(), card.getEmpno());
        assertEquals(marker, card.getCardNumber());
        assertEquals(LocalDate.of(2026, 3, 1), card.getIssuedOn());
        SQLException duplicate = assertThrows(SQLException.class, () -> execute(
                "INSERT INTO emp_card (empno, card_number, issued_on) VALUES (?, ?, ?)",
                holder.getEmpno(), marker + "other", java.sql.Date.valueOf("2026-03-02")));
        assertEquals("23000", duplicate.getSQLState());
        assertEquals(1, employees.deleteById(holder.getEmpno()));
        assertNull(employees.selectByIdWithCard(holder.getEmpno()));
    }

    @Test
    void duplicateMembershipIsRejectedAndRollbackRemovesTheWholeGraph() throws Exception {
        Dept department = insertDept();
        Emp employee = insertEmp(department.getDeptno(), "rollback");
        Project project = project("rollback");
        member(project, employee);
        SQLException duplicate = assertThrows(SQLException.class, () -> member(project, employee));
        assertEquals("23000", duplicate.getSQLState());
        session.rollback();
        try (var fresh = MyBatisUtil.openSession()) {
            assertNull(fresh.getMapper(ProjectMapper.class).selectByIdWithEmps(project.getProjectId()));
            assertNull(fresh.getMapper(EmpMapper.class).selectByIdWithProjects(employee.getEmpno()));
        }
    }
}
