package com.mapper;

import com.entity.Emp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 员工 CRUD，以及员工到部门的多对一查询。 */
public interface EmpMapper {
    List<Emp> selectAll();

    Emp selectById(@Param("empno") Integer empno);

    Emp selectByIdWithDept(@Param("empno") Integer empno);

    Emp selectByIdWithCard(@Param("empno") Integer empno);

    Emp selectByIdWithProjects(@Param("empno") Integer empno);

    List<Emp> selectByDeptno(@Param("deptno") Integer deptno);

    int insert(Emp emp);

    int update(Emp emp);

    /** 逻辑删除，保留原始数据。 */
    int deleteById(@Param("empno") Integer empno);
}
