package com.employee;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.employee.entity.Employee;
import com.employee.mapper.EmployeePlusMapper;
import com.employee.query.EmployeeQueries;
import com.employee.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

/** 任务四演示：BaseMapper CRUD、QueryWrapper/Lambda 条件查询与每页 3 条的分页。 */
public final class MyBatisPlusDemo {
    private MyBatisPlusDemo() {
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "all" : args[0];
        if (!Arrays.asList("all", "crud", "query", "page").contains(mode)) {
            throw new IllegalArgumentException("参数应为 all、crud、query 或 page");
        }
        try (SqlSession session = MyBatisUtil.openSession()) {
            EmployeePlusMapper mapper = session.getMapper(EmployeePlusMapper.class);
            try {
                if ("all".equals(mode) || "crud".equals(mode)) {
                    demonstrateCrud(mapper);
                }
                if ("all".equals(mode) || "query".equals(mode)) {
                    System.out.println("QueryWrapper（姓名含刘、性别男、工资至少5000元、在职）：");
                    mapper.selectList(EmployeeQueries.queryWrapper("刘", "男", new BigDecimal("5000.00")))
                            .forEach(System.out::println);
                    System.out.println("LambdaQueryWrapper（相同条件）：");
                    mapper.selectList(EmployeeQueries.lambdaQueryWrapper("刘", "男", new BigDecimal("5000.00")))
                            .forEach(System.out::println);
                }
                if ("all".equals(mode) || "page".equals(mode)) {
                    Page<Employee> result = mapper.selectPage(new Page<>(1, 3),
                            EmployeeQueries.lambdaQueryWrapper(null, null, null));
                    System.out.println("第 " + result.getCurrent() + " 页，每页 " + result.getSize() + " 条");
                    System.out.println("总记录数 = " + result.getTotal());
                    System.out.println("总页数 = " + result.getPages());
                    result.getRecords().forEach(System.out::println);
                }
            } finally {
                // 实验不保存临时记录，正式业务需要持久化时才调用 commit()。
                session.rollback();
            }
        }
    }

    private static void demonstrateCrud(EmployeePlusMapper mapper) {
        Employee emp = new Employee("MP通用CRUD演示", "mpdemo" + UUID.randomUUID().toString().replace("-", ""),
                "男", new BigDecimal("6500.00"), "Java工程师", LocalDate.of(2024, 1, 1), 1);
        System.out.println("MP插入行数 = " + mapper.insert(emp) + "，回填主键 = " + emp.getEmpId());
        System.out.println("MP按主键查询 = " + mapper.selectById(emp.getEmpId()));
        Employee patch = new Employee();
        patch.setEmpId(emp.getEmpId());
        patch.setSalary(new BigDecimal("7500.00"));
        patch.setPost("高级Java工程师");
        System.out.println("MP按主键更新行数 = " + mapper.updateById(patch));
        System.out.println("MP更新后查询 = " + mapper.selectById(emp.getEmpId()));
        System.out.println("MP按主键删除行数 = " + mapper.deleteById(emp.getEmpId()));
        System.out.println("MP删除后查询 = " + mapper.selectById(emp.getEmpId()));
    }
}
