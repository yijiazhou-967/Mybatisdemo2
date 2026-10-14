package com.console;

import com.entity.Dept;
import com.entity.Emp;
import com.entity.EmployeeCard;
import com.entity.Project;
import com.mapper.DeptMapper;
import com.mapper.EmpMapper;
import com.mapper.ProjectMapper;
import com.util.MyBatisUtil;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Supplier;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** 交互式关联查询，每次查询使用独立会话，结果读取后立即关闭。 */
public final class RelationshipConsole {
    private static final DateTimeFormatter CHINESE_DATE = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private final Supplier<SqlSession> sessions;

    public RelationshipConsole(Supplier<SqlSession> sessions) {
        this.sessions = sessions;
    }

    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains("--help")) {
            System.out.println("运行 query.cmd 进入菜单；query.cmd --init 初始化数据；--sql 显示 SQL 日志。");
            return;
        }
        for (String arg : args) {
            if (!Arrays.asList("--init", "--sql").contains(arg)) {
                throw new IllegalArgumentException("参数仅支持 --init、--sql 或 --help");
            }
        }
        if (Arrays.asList(args).contains("--init")) {
            RelationshipData.initialize();
            System.out.println("部门、员工、项目及工牌实验数据初始化完成，可运行 query.cmd 查询。");
            return;
        }
        if (!Arrays.asList(args).contains("--sql")) {
            Logger.getLogger("com.mapper").setLevel(Level.INFO);
        }
        new RelationshipConsole(MyBatisUtil::openSession).run(
                new InputStreamReader(System.in, StandardCharsets.UTF_8), new PrintWriter(System.out, true));
    }

    public void run(Reader source, PrintWriter out) throws IOException {
        BufferedReader input = source instanceof BufferedReader ? (BufferedReader) source : new BufferedReader(source);
        out.println("员工与项目关联查询终端");
        while (true) {
            out.println("\n1. 通过项目查员工（多对多）");
            out.println("2. 通过员工查项目（多对多）");
            out.println("3. 通过员工查工牌（一对一）");
            out.println("4. 通过员工查部门（多对一）");
            out.println("5. 通过部门查员工（一对多）");
            out.println("6. 查看项目、员工和部门编号");
            out.println("0. 退出");
            out.print("请选择：");
            out.flush();
            String choice = input.readLine();
            if (choice != null) choice = choice.replace("\uFEFF", "").trim();
            if (choice == null || "0".equals(choice)) {
                out.println("查询终端已退出。");
                return;
            }
            if (!choice.matches("[1-6]")) {
                out.println("请输入 0 到 6 的菜单编号。");
                continue;
            }
            Integer id = null;
            if (!"6".equals(choice)) {
                String label = "1".equals(choice) ? "项目" : "5".equals(choice) ? "部门" : "员工";
                id = readId(input, out, label);
                if (id == null) {
                    out.println("输入结束，查询终端已退出。");
                    return;
                }
            }
            try (SqlSession session = sessions.get()) {
                query(session, choice, id, out);
            } catch (PersistenceException | IllegalStateException ex) {
                out.println("查询失败，请检查 MySQL 和 db-local.properties；首次运行请先执行 query.cmd --init。");
            }
        }
    }

    private Integer readId(BufferedReader input, PrintWriter out, String label) throws IOException {
        while (true) {
            out.print("请输入" + label + "编号：");
            out.flush();
            String text = input.readLine();
            if (text == null) return null;
            try {
                int id = Integer.parseInt(text.trim());
                if (id > 0) return id;
            } catch (NumberFormatException ignored) {
                // 输入错误留在当前提示，可继续输入，也可用 EOF 退出。
            }
            out.println("编号必须为正整数，请重新输入。");
        }
    }

    private void query(SqlSession session, String choice, Integer id, PrintWriter out) {
        EmpMapper employees = session.getMapper(EmpMapper.class);
        DeptMapper departments = session.getMapper(DeptMapper.class);
        ProjectMapper projects = session.getMapper(ProjectMapper.class);
        switch (choice) {
            case "1":
                Project project = projects.selectByIdWithEmps(id);
                if (project == null) { out.println("未找到该项目。"); break; }
                printProject(project, out);
                if (project.getEmps().isEmpty()) out.println("该项目暂无员工。");
                else project.getEmps().forEach(emp -> printEmployee(emp, out));
                break;
            case "2":
                Emp participant = employees.selectByIdWithProjects(id);
                if (participant == null) { out.println("未找到该员工。"); break; }
                printEmployee(participant, out);
                if (participant.getProjects().isEmpty()) out.println("该员工暂未参与项目。");
                else participant.getProjects().forEach(item -> printProject(item, out));
                break;
            case "3":
                Emp holder = employees.selectByIdWithCard(id);
                if (holder == null) { out.println("未找到该员工。"); break; }
                printEmployee(holder, out);
                EmployeeCard card = holder.getCard();
                if (card == null) out.println("该员工尚未办理工牌。");
                else out.printf("工牌号：%s | 发放日期：%s%n", card.getCardNumber(), dateText(card.getIssuedOn()));
                break;
            case "4":
                Emp employee = employees.selectByIdWithDept(id);
                if (employee == null) { out.println("未找到该员工。"); break; }
                printEmployee(employee, out);
                if (employee.getDept() == null) out.println("该员工没有可查询的所属部门。");
                else printDepartment(employee.getDept(), out);
                break;
            case "5":
                Dept department = departments.selectByIdWithEmps(id);
                if (department == null) { out.println("未找到该部门。"); break; }
                printDepartment(department, out);
                if (department.getEmps().isEmpty()) out.println("该部门暂无员工。");
                else department.getEmps().forEach(emp -> printEmployee(emp, out));
                break;
            case "6":
                out.println("--- 项目 ---");
                projects.selectAll().forEach(item -> printProject(item, out));
                out.println("--- 员工 ---");
                employees.selectAll().forEach(emp -> printEmployee(emp, out));
                out.println("--- 部门 ---");
                departments.selectAll().forEach(dept -> printDepartment(dept, out));
                break;
            default: throw new IllegalArgumentException("未知查询类型");
        }
        out.flush();
    }

    private void printEmployee(Emp emp, PrintWriter out) {
        out.printf("员工编号：%d | 姓名：%s | 职位：%s | 薪水：%s%n",
                emp.getEmpno(), emp.getEname(), emp.getJob() == null ? "未填写" : emp.getJob(),
                emp.getSal() == null ? "未填写" : String.format(Locale.ROOT, "%.2f 元", emp.getSal()));
    }

    private void printProject(Project project, PrintWriter out) {
        out.printf("项目编号：%d | 编码：%s | 项目：%s | 启动日期：%s%n",
                project.getProjectId(), project.getProjectCode(), project.getTitle(),
                dateText(project.getStartDate()));
    }

    private void printDepartment(Dept dept, PrintWriter out) {
        out.printf("部门编号：%d | 部门：%s | 地点：%s%n", dept.getDeptno(), dept.getDname(), dept.getLoc());
    }

    private String dateText(LocalDate date) {
        return date == null ? "未填写" : CHINESE_DATE.format(date);
    }
}
