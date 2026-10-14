package com.employee.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.employee.entity.Employee;

import java.math.BigDecimal;

/** XML 与 MP 共用条件模型；相同工资用主键排序保证翻页稳定。 */
public final class EmployeeQueries {
    private EmployeeQueries() {
    }

    public static QueryWrapper<Employee> queryWrapper(String name, String gender, BigDecimal minSalary) {
        return queryWrapper(activeSearch(name, gender, minSalary));
    }

    public static LambdaQueryWrapper<Employee> lambdaQueryWrapper(String name, String gender, BigDecimal minSalary) {
        return lambdaQueryWrapper(activeSearch(name, gender, minSalary));
    }

    private static EmployeeSearch activeSearch(String name, String gender, BigDecimal minimum) {
        return new EmployeeSearch().name(name).gender(gender).status(1).salaryBetween(minimum, null);
    }

    public static QueryWrapper<Employee> queryWrapper(EmployeeSearch criteria) {
        EmployeeSearch q = checked(criteria);
        QueryWrapper<Employee> sql = new QueryWrapper<>();
        if (q.getDepartment() != null) sql.eq("department", q.getDepartment());
        if (q.getStatus() != null) sql.eq("work_status", q.getStatus());
        if (q.getMinimumSalary() != null) sql.ge("monthly_salary", q.getMinimumSalary());
        if (q.getMaximumSalary() != null) sql.le("monthly_salary", q.getMaximumSalary());
        if (q.getHiredFrom() != null) sql.ge("entry_date", q.getHiredFrom());
        if (q.getHiredThrough() != null) sql.le("entry_date", q.getHiredThrough());
        if (q.getOfficeLocation() != null) sql.eq("office_location", q.getOfficeLocation());
        if (q.getGender() != null) sql.eq("gender", q.getGender());
        if (q.getName() != null) sql.like("full_name", q.getName());
        sql.orderByDesc("monthly_salary").orderByAsc("staff_id");
        return sql;
    }

    public static LambdaQueryWrapper<Employee> lambdaQueryWrapper(EmployeeSearch criteria) {
        EmployeeSearch q = checked(criteria);
        LambdaQueryWrapper<Employee> sql = new LambdaQueryWrapper<>();
        if (q.getDepartment() != null) sql.eq(Employee::getDepartment, q.getDepartment());
        if (q.getStatus() != null) sql.eq(Employee::getStatus, q.getStatus());
        if (q.getMinimumSalary() != null) sql.ge(Employee::getSalary, q.getMinimumSalary());
        if (q.getMaximumSalary() != null) sql.le(Employee::getSalary, q.getMaximumSalary());
        if (q.getHiredFrom() != null) sql.ge(Employee::getHireDate, q.getHiredFrom());
        if (q.getHiredThrough() != null) sql.le(Employee::getHireDate, q.getHiredThrough());
        if (q.getOfficeLocation() != null) sql.eq(Employee::getOfficeLocation, q.getOfficeLocation());
        if (q.getGender() != null) sql.eq(Employee::getGender, q.getGender());
        if (q.getName() != null) sql.like(Employee::getName, q.getName());
        sql.orderByDesc(Employee::getSalary).orderByAsc(Employee::getEmpId);
        return sql;
    }

    private static EmployeeSearch checked(EmployeeSearch criteria) {
        return (criteria == null ? new EmployeeSearch() : criteria).validate();
    }
}
