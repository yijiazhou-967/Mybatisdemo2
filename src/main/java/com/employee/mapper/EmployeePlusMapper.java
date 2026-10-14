package com.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.employee.entity.Employee;

/**
 * 任务四：无需 XML 即获得通用 CRUD、条件查询与分页。
 * 与任务二、三的 EmployeeMapper 共用表和会话，使用不同命名空间避免同名 SQL 冲突。
 */
public interface EmployeePlusMapper extends BaseMapper<Employee> {
}
