package com.employee.query;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 查询条件与员工档案分离；金额、日期范围均包含端点。 */
public final class EmployeeSearch {
    private String name;
    private String department;
    private String gender;
    private Integer status;
    private String officeLocation;
    private BigDecimal minimumSalary;
    private BigDecimal maximumSalary;
    private LocalDate hiredFrom;
    private LocalDate hiredThrough;

    public EmployeeSearch name(String value) { name = clean(value); return this; }
    public EmployeeSearch department(String value) { department = clean(value); return this; }
    public EmployeeSearch gender(String value) { gender = clean(value); return this; }
    public EmployeeSearch status(Integer value) { status = value; return this; }
    public EmployeeSearch office(String value) { officeLocation = clean(value); return this; }

    public EmployeeSearch salaryBetween(BigDecimal minimum, BigDecimal maximum) {
        minimumSalary = minimum;
        maximumSalary = maximum;
        return this;
    }

    public EmployeeSearch hiredBetween(LocalDate first, LocalDate last) {
        hiredFrom = first;
        hiredThrough = last;
        return this;
    }

    /** 在 Mapper/Wrapper 入口调用，错误范围不会进入数据库。 */
    public EmployeeSearch validate() {
        if (status != null && status != 0 && status != 1) {
            throw new IllegalArgumentException("在职状态只能为 0 或 1");
        }
        if (gender != null && !"男".equals(gender) && !"女".equals(gender)) {
            throw new IllegalArgumentException("性别条件应为男或女");
        }
        if ((minimumSalary != null && minimumSalary.signum() < 0)
                || (maximumSalary != null && maximumSalary.signum() < 0)) {
            throw new IllegalArgumentException("工资条件不能为负数");
        }
        if (minimumSalary != null && maximumSalary != null
                && minimumSalary.compareTo(maximumSalary) > 0) {
            throw new IllegalArgumentException("最低工资不能超过最高工资");
        }
        if (hiredFrom != null && hiredThrough != null && hiredFrom.isAfter(hiredThrough)) {
            throw new IllegalArgumentException("入职起始日期不能晚于结束日期");
        }
        return this;
    }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getGender() { return gender; }
    public Integer getStatus() { return status; }
    public String getOfficeLocation() { return officeLocation; }
    public BigDecimal getMinimumSalary() { return minimumSalary; }
    public BigDecimal getMaximumSalary() { return maximumSalary; }
    public LocalDate getHiredFrom() { return hiredFrom; }
    public LocalDate getHiredThrough() { return hiredThrough; }
}
