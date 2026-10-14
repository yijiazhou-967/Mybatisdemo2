package com.employee;

/** 一次运行展示任务二至四；任务一配置由 Maven 和 sql/init.sql 完成。 */
public final class ExperimentOneDemo {
    private ExperimentOneDemo() { }

    public static void main(String[] args) {
        if (args.length > 0 && "help".equals(args[0])) {
            System.out.println("先执行 sql/init.sql 并配置 jdbc-local.properties，再无参数运行本类。");
            return;
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("本入口仅接受无参数或 help");
        }
        System.out.println("=== 任务二：XML CRUD；任务三：条件、局部更新与批量操作 ===");
        EmployeeDemo.main(new String[]{"all"});
        System.out.println("=== 任务四：BaseMapper CRUD、两种构造器与分页 ===");
        MyBatisPlusDemo.main(new String[]{"all"});
    }
}
