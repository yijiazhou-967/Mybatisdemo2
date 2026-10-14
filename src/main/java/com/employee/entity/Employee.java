package com.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工档案实体，对应自定义表 ssm_emp.staff_info。
 * 字段采用驼峰命名，配合全局配置 mapUnderscoreToCamelCase 自动映射下划线列。
 */
@TableName("staff_info")
public class Employee {

    /** 员工编号（主键，自增） */
    @TableId(value = "staff_id", type = IdType.AUTO)
    private Integer empId;
    /** 员工姓名 */
    @TableField("full_name")
    private String name;
    /** 部门 */
    private String department;
    /** 性别 */
    private String gender;
    /** 工资（元） */
    @TableField("monthly_salary")
    private BigDecimal salary;
    /** 岗位 */
    @TableField("job_title")
    private String post;
    /** 入职日期 */
    @TableField("entry_date")
    private LocalDate hireDate;
    /** 状态：1 在职 / 0 离职；属于业务状态，不作为逻辑删除标记 */
    @TableField("work_status")
    private Integer status;
    /** 工作邮箱，可为空，已填写的邮箱不可重复 */
    private String email;
    /** 办公地点 */
    @TableField("office_location")
    private String officeLocation;

    /** 无参构造：MyBatis 结果映射实例化对象所必需 */
    public Employee() {
    }

    /** 便捷构造：不含自增主键，便于快速创建待插入对象 */
    public Employee(String name, String department, String gender,
                 BigDecimal salary, String post, LocalDate hireDate, Integer status) {
        this.name = name;
        this.department = department;
        this.gender = gender;
        this.salary = salary;
        this.post = post;
        this.hireDate = hireDate;
        this.status = status;
    }

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public String getPost() {
        return post;
    }

    public void setPost(String post) {
        this.post = post;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOfficeLocation() {
        return officeLocation;
    }

    public void setOfficeLocation(String officeLocation) {
        this.officeLocation = officeLocation;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "empId=" + empId +
                ", name='" + name + '\'' +
                ", department='" + department + '\'' +
                ", gender='" + gender + '\'' +
                ", salary=" + salary +
                ", post='" + post + '\'' +
                ", hireDate=" + hireDate +
                ", status=" + status +
                ", email='" + email + '\'' +
                ", officeLocation='" + officeLocation + '\'' +
                '}';
    }
}
