package com.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.employee.entity.Employee;
import com.employee.query.EmployeeQueries;
import com.employee.query.EmployeeSearch;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 任务四：无需 XML 即获得通用 CRUD、条件查询与分页。
 * 与任务二、三的 EmployeeMapper 共用表和会话，使用不同命名空间避免同名 SQL 冲突。
 */
public interface EmployeePlusMapper extends BaseMapper<Employee> {
    /** 统一分页入口，拒绝无效页码以及绕过插件限制的过大页面。 */
    default Page<Employee> findPage(EmployeeSearch criteria, long number, long size) {
        if (number < 1 || size < 1 || size > 100) {
            throw new IllegalArgumentException("页码必须从 1 开始，每页条数必须在 1 到 100 之间");
        }
        return selectPage(new Page<>(number, size), EmployeeQueries.lambdaQueryWrapper(criteria));
    }
}
