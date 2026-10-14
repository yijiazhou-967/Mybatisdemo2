package com.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.util.List;

/**
 * 部门实体类（dept 表）
 *
 * 与 emp 表是一对多关系：一个部门有多个员工
 *
 * @version 1.0
 */
public class Dept implements Serializable {

    private Integer deptno;  // 部门编号（主键）
    private String dname;     // 部门名称
    private String loc;       // 部门位置

    // 逻辑删除字段（0=未删除，1=已删除）
    @TableLogic
    private Integer deleted;

    // 一对多关系：一个部门有多个员工
    private List<Emp> emps;  // 员工列表

    // 构造方法
    public Dept() {
    }

    public Dept(Integer deptno, String dname, String loc) {
        this.deptno = deptno;
        this.dname = dname;
        this.loc = loc;
    }

    // getter 和 setter 方法
    public Integer getDeptno() {
        return deptno;
    }

    public void setDeptno(Integer deptno) {
        this.deptno = deptno;
    }

    public String getDname() {
        return dname;
    }

    public void setDname(String dname) {
        this.dname = dname;
    }

    public String getLoc() {
        return loc;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public List<Emp> getEmps() {
        return emps;
    }

    public void setEmps(List<Emp> emps) {
        this.emps = emps;
    }

    // toString 方法
    @Override
    public String toString() {
        return "Dept{" +
                "deptno=" + deptno +
                ", dname='" + dname + '\'' +
                ", loc='" + loc + '\'' +
                '}';
    }
}
