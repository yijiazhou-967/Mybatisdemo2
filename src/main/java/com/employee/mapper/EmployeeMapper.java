package com.employee.mapper;

import com.employee.entity.Employee;
import com.employee.query.EmployeeSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工持久层接口，对应映射文件 EmployeeMapper.xml。
 * 任务二：基础 CRUD；任务三：动态 SQL、多条件查询与批量操作。
 */
public interface EmployeeMapper {

    // ============ 任务二：基础 CRUD ============

    /** 查询全部员工 */
    List<Employee> selectAll();

    /** 按主键查询单条员工 */
    Employee selectById(@Param("empId") Integer empId);

    /** 新增员工，自增主键回填到 empId */
    int insert(Employee emp);

    /** 按主键整体更新员工 */
    int update(Employee emp);

    /** 按主键删除员工 */
    int deleteById(@Param("empId") Integer empId);

    // ============ 任务三：动态 SQL ============

    /** 姓名模糊，部门/性别/状态精确，条件可为空 */
    List<Employee> selectByCondition(Employee condition);

    /** 与 where 写法等效的 trim 演示，便于核对结果 */
    List<Employee> selectByConditionWithTrim(Employee condition);

    /** 独立查询模型：加入工资区间、入职日期区间及办公地点。 */
    default List<Employee> search(EmployeeSearch criteria) {
        return searchRows((criteria == null ? new EmployeeSearch() : criteria).validate());
    }

    default List<Employee> searchWithTrim(EmployeeSearch criteria) {
        return searchTrimRows((criteria == null ? new EmployeeSearch() : criteria).validate());
    }

    List<Employee> searchRows(@Param("q") EmployeeSearch criteria);

    List<Employee> searchTrimRows(@Param("q") EmployeeSearch criteria);

    /** 动态更新：仅更新非空字段（set 标签） */
    default int updateSelective(Employee emp) {
        if (emp == null || emp.getEmpId() == null || emp.getEmpId() <= 0) {
            throw new IllegalArgumentException("动态更新必须提供有效的员工主键");
        }
        if (emp.getName() == null && emp.getDepartment() == null && emp.getGender() == null
                && emp.getSalary() == null && emp.getPost() == null
                && emp.getHireDate() == null && emp.getStatus() == null
                && emp.getEmail() == null && emp.getOfficeLocation() == null) {
            throw new IllegalArgumentException("动态更新必须提供至少一个待更新字段");
        }
        return updateSelectiveRows(emp);
    }

    /** XML 执行入口；业务调用使用带校验的 updateSelective */
    int updateSelectiveRows(Employee emp);

    /** 批量删除（foreach 标签） */
    default int deleteBatch(List<Integer> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("删除主键列表不能为 null");
        }
        if (ids.isEmpty()) {
            return 0;
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("删除列表必须全部为有效主键");
        }
        return deleteBatchRows(ids);
    }

    /** XML 执行入口；业务调用使用带校验的 deleteBatch */
    int deleteBatchRows(@Param("ids") List<Integer> ids);

    /** 批量插入（foreach 标签） */
    default int insertBatch(List<Employee> employees) {
        if (employees == null) {
            throw new IllegalArgumentException("插入员工列表不能为 null");
        }
        if (employees.isEmpty()) {
            return 0;
        }
        if (employees.stream().anyMatch(emp -> emp == null)) {
            throw new IllegalArgumentException("插入列表不能包含 null 员工");
        }
        return insertBatchRows(employees);
    }

    /** XML 执行入口；业务调用使用带校验的 insertBatch */
    int insertBatchRows(@Param("employees") List<Employee> employees);

    /** 分支查询：优先按姓名精确，其次按部门精确，否则查询全部 */
    List<Employee> selectSmart(Employee condition);
}
