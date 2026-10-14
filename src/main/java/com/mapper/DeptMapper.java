package com.mapper;

import com.entity.Dept;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 部门 CRUD，以及部门到员工的一对多查询。 */
public interface DeptMapper {
    List<Dept> selectAll();

    Dept selectById(@Param("deptno") Integer deptno);

    Dept selectByIdWithEmps(@Param("deptno") Integer deptno);

    int insert(Dept dept);

    int update(Dept dept);

    /** 逻辑删除，保留原始数据。 */
    int deleteById(@Param("deptno") Integer deptno);
}
