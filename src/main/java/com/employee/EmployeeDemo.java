package com.employee;

import com.employee.entity.Employee;
import com.employee.mapper.EmployeeMapper;
import com.employee.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 第一个 MyBatis 程序（任务二 步骤 2.4）。
 * 演示：读取配置 -> 构建 SqlSessionFactory（工具类封装）-> 开启会话 -> 获取 Mapper 代理 -> 执行查询。
 */
public class EmployeeDemo {

    public static void main(String[] args) {
        String mode = args.length == 0 ? "list" : args[0];
        if (!Arrays.asList("list", "crud", "dynamic").contains(mode)) {
            throw new IllegalArgumentException("参数应为 list、crud 或 dynamic");
        }
        // try-with-resources：会话用完自动关闭
        try (SqlSession session = MyBatisUtil.openSession()) {
            EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);

            try {
                if ("crud".equals(mode)) {
                    demonstrateCrud(mapper);
                } else if ("dynamic".equals(mode)) {
                    demonstrateDynamic(mapper);
                } else {
                    List<Employee> employees = mapper.selectAll();
                    System.out.println("员工总数：" + employees.size());
                    employees.forEach(System.out::println);
                }
            } finally {
                // 练习程序不持久保存临时记录；正式业务增删改须显式 session.commit()。
                session.rollback();
            }
        }
    }

    private static Employee example(String name, String department) {
        Employee employee = new Employee(name, department, "男", new BigDecimal("6500.00"),
                "Java工程师", LocalDate.of(2024, 1, 1), 1);
        employee.setEmail(UUID.randomUUID().toString() + "@company.example");
        employee.setOfficeLocation("杭州研发中心");
        return employee;
    }

    private static void demonstrateCrud(EmployeeMapper mapper) {
        Employee emp = example("CRUD演示", "demo" + UUID.randomUUID().toString().replace("-", ""));
        System.out.println("插入行数：" + mapper.insert(emp) + "，回填主键：" + emp.getEmpId());
        System.out.println("按主键查询：" + mapper.selectById(emp.getEmpId()));
        emp.setSalary(new BigDecimal("7500.00"));
        System.out.println("整体更新行数：" + mapper.update(emp));
        System.out.println("更新后查询：" + mapper.selectById(emp.getEmpId()));
        System.out.println("删除行数：" + mapper.deleteById(emp.getEmpId()));
        System.out.println("删除后查询：" + mapper.selectById(emp.getEmpId()));
    }

    private static void demonstrateDynamic(EmployeeMapper mapper) {
        String department = "demo" + UUID.randomUUID().toString().replace("-", "");
        List<Employee> batch = Arrays.asList(example("动态SQL演示A", department), example("动态SQL演示B", department));
        System.out.println("批量插入行数：" + mapper.insertBatch(batch));
        Employee condition = new Employee();
        condition.setDepartment(department);
        condition.setGender("男");
        condition.setStatus(1);
        List<Employee> found = mapper.selectByCondition(condition);
        System.out.println("where 多条件查询：" + found);
        System.out.println("trim 等效查询：" + mapper.selectByConditionWithTrim(condition));
        Employee patch = new Employee();
        patch.setEmpId(found.get(0).getEmpId());
        patch.setSalary(new BigDecimal("7000.00"));
        patch.setStatus(0);
        System.out.println("动态更新行数：" + mapper.updateSelective(patch));
        Employee branch = new Employee();
        branch.setDepartment(department);
        System.out.println("choose 部门分支：" + mapper.selectSmart(branch));
        System.out.println("批量删除行数：" + mapper.deleteBatch(found.stream()
                .map(Employee::getEmpId).collect(Collectors.toList())));
    }
}
