# MyBatis 部门员工与员工档案实验

## 部门与员工多表查询（本次导入）

提供的 `Dept.java`、`Emp.java` 已按原包名 `com.entity` 导入；原始 `dept_emp.sql` 保存在 `sql/dept_emp.sql`。

| 文件 | 用途 |
| --- | --- |
| `src/main/resources/db.properties` | `dept_emp_db` 数据库连接默认配置 |
| `src/main/resources/db-local.properties` | 本机账号口令，已被 Git 忽略 |
| `src/main/resources/mybatis-config.xml` | 加载数据库配置、实体别名和两个 Mapper |
| `src/main/resources/log4j.properties` | 控制台 SQL、绑定参数和结果行数日志 |
| `src/main/java/com/util/MyBatisUtil.java` | 读取配置、共享会话工厂、创建 SqlSession |
| `src/main/java/com/mapper/DeptMapper.java` | 部门 CRUD 和一对多员工查询 |
| `src/main/java/com/mapper/EmpMapper.java` | 员工 CRUD 和多对一部门查询 |
| `src/main/resources/com/mapper/DeptMapper.xml` | 部门 SQL 与 collection 映射 |
| `src/main/resources/com/mapper/EmpMapper.xml` | 员工 SQL 与 association 映射 |
| `src/test/java/com/mapper/DeptMapperTest.java` | 部门 CRUD、一对多、空部门和逻辑删除测试 |
| `src/test/java/com/mapper/EmpMapperTest.java` | 员工 CRUD、多对一、空部门引用和事务回滚测试 |
| `src/test/java/com/mapper/MapperConfigurationTest.java` | 不连接数据库的配置与参数绑定测试 |

先在 MySQL 客户端执行 `source sql/dept_emp_init.sql;`，再运行以下测试，或在 IDEA 中直接运行两个 Mapper 测试类：

```powershell
.\mvnw.cmd -Pdatabase-tests "-Dtest=com.mapper.*Test" test
```

原始 SQL 包含 `DROP TABLE`，项目实际初始化使用独立的 `dept_emp_db` 和可重复执行的 `dept_emp_init.sql`。该脚本保留原始 4 个部门、14 名员工，并补充两个实体已有的 `deleted` 列；已有同名表不会自动修改结构。传统 MyBatis 的 XML 显式实现 `deleted=0` 查询过滤及 `deleted=1` 逻辑删除。关联查询使用 LEFT JOIN，空部门返回空员工列表，员工未分配部门时关联对象为 null。

本地配置优先级：JVM 的 `db.*` 属性 → `DEPT_EMP_DB_*` 环境变量 → `db-local.properties` → `db.properties`。日志由 [reload4j](https://reload4j.qos.ch/) 读取 Log4j 1.x 格式的 `log4j.properties`，SQL 日志级别为 DEBUG。2026-10-15 新增 12 项测试及原有 34 项测试全部通过；测试中的临时记录均回滚。

## 员工档案实验（原有功能）

课程：《JAVA框架技术（一）》。项目使用 `ssm_emp.staff_info` 企业员工档案表，自定义字段和样例数据，并完成实验一的基础 CRUD、动态 SQL、批量操作、MyBatis-Plus 条件查询与分页，以及任务五故障演练。

这是 Java 控制台项目。运行入口为 `com.employee.EmployeeDemo`，结果与 SQL 日志显示在控制台；无需通过 Tomcat 部署。原员工档案功能读取 `employee-mybatis-config.xml`、`jdbc.properties` 和 `jdbc-local.properties`，连接 `ssm_emp.staff_info`。

## 快速运行（IDEA）

1. 用 IDEA 打开项目，重新加载 Maven 依赖，使用 JDK 11 或 17。
2. 启动 MySQL 8.0，执行 `sql/init.sql` 创建员工库、自定义档案表与 5 条样例。
3. 核对 `src/main/resources/jdbc-local.properties` 中的账号、密码和数据库 URL。当前连接的数据库名为 `ssm_emp`；新检出项目可复制 `jdbc.properties` 为 `jdbc-local.properties` 后填写。
4. 在 IDEA 的运行配置下拉框选择 `EmployeeDemo` 并运行；也可打开 `src/main/java/com/employee/EmployeeDemo.java`，点击 `main` 左侧的绿色运行按钮。

项目提供 `.run/EmployeeDemo.run.xml` 和 `.run/MyBatisPlusDemo.run.xml`。如果 IDEA 报模块不存在，重新加载 Maven，并在配置的“使用模块的类路径”中选择当前项目模块。旧的 Tomcat/WAR 或 `MusicStoreDemo` 配置不适用于当前入口。

在项目根目录终端启动 MySQL 客户端：

```powershell
mysql --default-character-set=utf8mb4 -u root -p
```

然后在 MySQL 命令行执行：

```sql
source sql/init.sql;
USE ssm_emp;
SHOW CREATE TABLE staff_info;
SELECT * FROM staff_info ORDER BY staff_id;
```

初始化脚本使用 `IF NOT EXISTS`，按姓名、部门和入职日期补齐缺失样例，可重复执行，不覆盖员工已修改的工资等字段。已有同名表的结构不会自动迁移。原 `ssm_emp.emp`、`employee_db` 和音乐库均保留，项目查询改用 `ssm_emp.staff_info`。

## 员工字段

| 数据库列 | Java 属性与类型 | 说明 |
| --- | --- | --- |
| `staff_id` | `empId` / Integer | 员工档案编号，自增主键 |
| `full_name` | `name` / String | 姓名，VARCHAR(60)，非空 |
| `department` | `department` / String | 部门，VARCHAR(60)，非空，普通索引 |
| `gender` | `gender` / String | 性别，CHAR(1)，默认男 |
| `monthly_salary` | `salary` / BigDecimal | 月薪，DECIMAL(10,2)，非空，默认 0.00 |
| `job_title` | `post` / String | 职位名称，VARCHAR(60)，可空 |
| `entry_date` | `hireDate` / LocalDate | 入职日期，可空 |
| `work_status` | `status` / Integer | 1 在职、0 离职，默认 1；业务状态 |
| `email` | `email` / String | 工作邮箱，VARCHAR(100)，可空，唯一索引 |
| `office_location` | `officeLocation` / String | 办公地点，VARCHAR(60)，可空 |

## 演示入口

`EmployeeDemo` 的 Program arguments：

| 参数 | 演示内容 |
| --- | --- |
| 留空或 `list` | 查询全部员工 |
| `crud` | 插入、主键回填、查询、整体更新、删除 |
| `dynamic` | 批量插入、where/trim 多条件查询、局部更新、choose 分支、批量删除 |

`MyBatisPlusDemo` 的 Program arguments：

| 参数 | 演示内容 |
| --- | --- |
| 留空或 `all` | 通用 CRUD、两种条件构造器和分页 |
| `crud` | BaseMapper 增删改查 |
| `query` | 姓名含刘、性别男、工资至少 5000 元且在职 |
| `page` | 在职员工第 1 页，每页 3 条，输出记录数与页数 |

新表中有陈嘉宁、刘子航、孙雨桐、周明远、许安然 5 条样例，其中 4 条在职；默认在职分页为 2 页，第一页 3 条。样例涵盖产品、技术、财务、运维、设计五个部门，以及上海、杭州、成都三处办公地点。邮箱使用示例域名。数据改动后以实际结果为准。

演示和数据库测试结束时回滚临时记录。正式业务需要保存修改时，在成功路径显式调用 `session.commit()`。SqlSessionFactory 共用，SqlSession 不跨线程共享。日志输出 `Preparing`、`Parameters`、`Total`；增删改还通过影响行数验证。

## 任务与代码

| 任务 | 文件与行为 |
| --- | --- |
| 一：环境与初始化 | `pom.xml`、`sql/init.sql`、Maven Wrapper、`docs/maven-settings-example.xml` |
| 二：实体、XML 与 CRUD | `Employee.java`、`EmployeeMapper.java`、`EmployeeMapper.xml`、`EmployeeDemo.java` |
| 三：动态 SQL | `if/where/trim/set/foreach/choose`，空批量返回 0，无效主键和空更新在执行前拒绝 |
| 四：MyBatis-Plus | `EmployeePlusMapper`、`EmployeeQueries`、`MyBatisPlusDemo`、MySQL 分页插件 |
| 五：排错与接口练习 | `MyBatisFaultDrillTest`、`docs/task5-lab-notes.md`、`docs/apifox/` |

Java 包名统一为 `com.employee`，Mapper XML 与接口位于相同包路径。实体用 `@TableName("staff_info")` 指定新表，通过 `@TableId` 和 `@TableField` 将现有 Java 属性映射到自定义列名。XML 的 `empMap` 覆盖全部 10 个字段。邮箱和办公地点支持普通插入、批量插入、整体更新和局部更新。

`selectByCondition` 和 `selectByConditionWithTrim` 支持姓名/部门模糊查询、性别/状态精确查询；空条件查全部。`selectSmart` 优先按姓名精确查询，其次按部门查询，否则查全部。`updateSelective` 仅更新非空字段，工资和状态为 0 时仍参与更新；入职日期置空使用整体 `update`。

`EmployeeQueries` 提供 QueryWrapper 与 LambdaQueryWrapper 两种等效写法，支持姓名、性别和最低工资，始终只查在职员工，并按工资降序、主键升序排序。离职状态不作为逻辑删除标记，仍能按 ID 查询。

## 连接配置

覆盖优先级：JVM 属性 `jdbc.*` → 环境变量 `EMPLOYEE_DB_*` → `jdbc-local.properties` → `jdbc.properties`。支持 `EMPLOYEE_DB_URL`、`EMPLOYEE_DB_USERNAME`、`EMPLOYEE_DB_PASSWORD`。本地口令文件被 Git 忽略。

默认 URL 指向 `localhost:3306/ssm_emp`，启用本机实验用的 `allowPublicKeyRetrieval=true`。资源文件按 UTF-8 保存。IDEA 的 Database 面板可添加 MySQL 数据源，Host 填 `localhost`、Port 填 `3306`、Database 填 `ssm_emp`，账号密码与 `jdbc-local.properties` 一致，点击 Test Connection 验证。

依赖保持课程版本：MP 3.5.3.1（提供 MyBatis 3.5.10）、Spring Core 5.3.15、MySQL Connector/J 8.4.0、SLF4J 2.0.13、Logback 1.5.6、JUnit Jupiter 5.10.2。`MyBatisUtil` 使用 `MybatisSqlSessionFactoryBuilder`，兼容原 XML 和 BaseMapper。

## 测试

默认 16 项配置、SQL 与参数校验测试无需连接数据库：

```powershell
.\mvnw.cmd test
```

执行 `sql/init.sql` 和 `sql/dept_emp_init.sql` 并配置两个数据库连接后，运行全部 46 项测试：

```powershell
.\mvnw.cmd -Pdatabase-tests test
```

已安装 Maven 时也可使用 `mvn test`、`mvn -Pdatabase-tests test`。首次使用 Wrapper 需要下载 Maven 3.9.6；依赖也需要联网下载。仅在依赖完整缓存且仓库 ID 匹配时使用 `-o`，不要为了离线运行直接覆盖全局 settings.xml。

数据库测试验证字段映射、主键回填、CRUD、动态 SQL、分页边界和事务回滚；各用例只操作独立标记的数据。MySQL 的自增序号在回滚后不会恢复，编号出现间隔是正常现象。报告位于 `target/surefire-reports/`。

2026-10-15 已连接本机 MySQL 8.0.44，创建 `ssm_emp.staff_info`，全部 34 项测试通过。`EmployeeDemo list` 实际查询得到 5 条新样例及邮箱、办公地点；重复执行初始化脚本后仍为 5 条。报告位于 `target/surefire-reports/`。测试使用本机缓存与 `target/employee-verification-settings.xml`，全局 Maven 配置未修改。

任务五单独运行：

```powershell
.\mvnw.cmd -Pdatabase-tests "-Dtest=MyBatisFaultDrillTest" test
```

该演练在 XML 内存副本中复现 `name/nAme` 参数拼写错误、`missing_name` 列映射错误和主键回填缺失，并对照验证修复；日志位于 `target/task5/fault-drills.log`。ApiFox 材料练习公开 GET 请求，项目尚未提供员工 HTTP 接口。

## 常见问题

| 现象 | 处理 |
| --- | --- |
| 部署源 `war exploded` 无效 | 将运行配置切换到 `EmployeeDemo` |
| Connection refused / Communications link failure | 核对 MySQL 服务、端口和 URL |
| Access denied | 核对本地账号和密码 |
| Unknown database / Table doesn't exist | 执行 `sql/init.sql`，确认连接 `ssm_emp` |
| Invalid bound statement | 核对接口、XML、namespace、扫描包及资源路径 |
| 离线依赖不可用 | 检查 Maven settings 和缓存来源仓库 ID，必要时联网补齐 |
| GenericTypeResolver 类找不到 | 重新加载 Maven，确认 Spring Core 依赖 |
| 分页返回全量数据 | 核对 PaginationInnerInterceptor 已注册 |
