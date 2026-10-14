package com.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.util.Date;

/**
 * 员工实体类（emp 表）
 *
 * 与 dept 表是多对一关系：多个员工属于一个部门
 *
 * @version 1.0
 */
public class Emp implements Serializable {

    private Integer empno;     // 员工编号（主键）
    private String ename;      // 员工姓名
    private String job;        // 职位
    private Integer mgr;       // 上级编号
    private Date hiredate;     // 入职日期
    private Double sal;        // 薪水
    private Double comm;       // 佣金
    private Integer deptno;    // 部门编号（外键）

    // 逻辑删除字段（0=未删除，1=已删除）
    @TableLogic
    private Integer deleted;

    // 多对一关系：一个员工属于一个部门
    private Dept dept;         // 部门对象

    // 构造方法
    public Emp() {
    }

    public Emp(Integer empno, String ename, String job, Integer mgr,
               Date hiredate, Double sal, Double comm, Integer deptno) {
        this.empno = empno;
        this.ename = ename;
        this.job = job;
        this.mgr = mgr;
        this.hiredate = hiredate;
        this.sal = sal;
        this.comm = comm;
        this.deptno = deptno;
    }

    // getter 和 setter 方法
    public Integer getEmpno() {
        return empno;
    }

    public void setEmpno(Integer empno) {
        this.empno = empno;
    }

    public String getEname() {
        return ename;
    }

    public void setEname(String ename) {
        this.ename = ename;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public Integer getMgr() {
        return mgr;
    }

    public void setMgr(Integer mgr) {
        this.mgr = mgr;
    }

    public Date getHiredate() {
        return hiredate;
    }

    public void setHiredate(Date hiredate) {
        this.hiredate = hiredate;
    }

    public Double getSal() {
        return sal;
    }

    public void setSal(Double sal) {
        this.sal = sal;
    }

    public Double getComm() {
        return comm;
    }

    public void setComm(Double comm) {
        this.comm = comm;
    }

    public Integer getDeptno() {
        return deptno;
    }

    public void setDeptno(Integer deptno) {
        this.deptno = deptno;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Dept getDept() {
        return dept;
    }

    public void setDept(Dept dept) {
        this.dept = dept;
    }

    // toString 方法
    @Override
    public String toString() {
        return "Emp{" +
                "empno=" + empno +
                ", ename='" + ename + '\'' +
                ", job='" + job + '\'' +
                ", mgr=" + mgr +
                ", hiredate=" + hiredate +
                ", sal=" + sal +
                ", comm=" + comm +
                ", deptno=" + deptno +
                '}';
    }
}
