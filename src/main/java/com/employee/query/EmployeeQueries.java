package com.employee.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.employee.entity.Employee;

import java.math.BigDecimal;

/** 任务四步骤 4.4：字符串字段与 Lambda 两种等效条件构造器写法。 */
public final class EmployeeQueries {
    private EmployeeQueries() {
    }

    /** 姓名模糊、性别精确、最低工资可选；只查在职，工资降序、主键升序。 */
    public static QueryWrapper<Employee> queryWrapper(String name, String gender, BigDecimal minSalary) {
        return new QueryWrapper<Employee>()
                .like(StringUtils.isNotBlank(name), "full_name", name)
                .eq(StringUtils.isNotBlank(gender), "gender", gender)
                .eq("work_status", 1)
                .ge(minSalary != null, "monthly_salary", minSalary)
                .orderByDesc("monthly_salary")
                .orderByAsc("staff_id");
    }

    /** Lambda 引用实体属性，避免手写字段名；每次调用创建独立构造器。 */
    public static LambdaQueryWrapper<Employee> lambdaQueryWrapper(String name, String gender, BigDecimal minSalary) {
        return new LambdaQueryWrapper<Employee>()
                .like(StringUtils.isNotBlank(name), Employee::getName, name)
                .eq(StringUtils.isNotBlank(gender), Employee::getGender, gender)
                .eq(Employee::getStatus, 1)
                .ge(minSalary != null, Employee::getSalary, minSalary)
                .orderByDesc(Employee::getSalary)
                .orderByAsc(Employee::getEmpId);
    }
}
