# 实验一任务五日志排查与接口测试记录

本任务基于员工项目完成三类 MyBatis 故障演练、公开 GET 请求准备和代码核查。故障及修复均使用正式 XML 的内存副本，在独立测试事务中执行；正式业务映射保持正确。ApiFox 桌面端的项目导入与用例保存按下面步骤完成。

## 步骤 5.1 三类故障与修复

运行数据库测试前启动 MySQL，确认本地连接配置有效。

```powershell
.\mvnw.cmd -Pdatabase-tests "-Dtest=MyBatisFaultDrillTest" test
```

代码：`src/test/java/com/employee/diagnostic/MyBatisFaultDrillTest.java`。日志：`target/task5/fault-drills.log`。JUnit 报告：`target/surefire-reports/com.employee.diagnostic.MyBatisFaultDrillTest.txt`。日志按 UTF-8 保存，包含 SQL、参数、真实异常堆栈、修复后的结果和回滚记录。

| 演练 | 故意制造的错误 | 实际观察 | 修复与验证 |
| --- | --- | --- | --- |
| 参数拼写 | INSERT 中将 `#{name}` 改为 `#{nAme}` | `PersistenceException` 包装 `ReflectionException`，缺少 nAme 的 getter；未成功执行插入 | 改回 `#{name}`，插入影响 1 行，主键和姓名正确 |
| 列映射 | resultMap 中 name 对应列 full_name 改为 `missing_name` | SELECT 返回 1 行，name 为 null，department 等字段仍有值；没有 SQL 语法异常 | 改回实际列 full_name，在同一事务查询得到正确姓名 |
| 主键回填 | 删除 INSERT 的 `useGeneratedKeys="true"` | INSERT 影响 1 行，对象 empId 为 null，但按独立部门标记查询可见数据库已生成主键 | 恢复 `useGeneratedKeys` 与 `keyProperty="empId"`，新对象主键正确回填 |

参数错误的关键日志摘录：

```text
ReflectionException: There is no getter for property named 'nAme' in 'class com.employee.entity.Employee'
```

指导书使用员工示例描述参数绑定异常；当前项目的单个实体参数在该 MyBatis 版本下实际产生以上反射异常。因此报告填写实际异常类型，不将它写成并未出现的 `ParameterBindingException`。

错误列名并不一定产生异常。本次错误结果映射仍得到一个对象，需要通过 name 是否为 null、其他字段是否正常和真实表结构定位问题。插入影响行数为 1 也不能证明主键已回填，必须分别检查对象主键和数据库记录。

三个用例先检查 InnoDB，再使用同一事务比较故障与修复版本。测试结束显式回滚，即使断言失败也会清理；出现预期异常是演练结果，JUnit 通过表示错误已被正确捕获且修复验证成功。每次运行的自增主键可能不同，不应在报告中写死某个 ID。

## 步骤 5.2 ApiFox GET 练习

### 练习文件

| 文件 | 用途 |
| --- | --- |
| `apifox/task5.postman_collection.json` | 首选导入文件，包含固定请求参数、请求头和 3 项断言 |
| `apifox/task5.openapi.json` | OpenAPI 3.0.3 接口定义，可作为另一种导入方式 |
| `apifox/task5-assertions.js` | 后置脚本；若导入后没有脚本，可手动粘贴 |
| `apifox/task5.http` | IDEA HTTP Client 等工具可读取的同一请求 |
| `apifox/http-verification.json` | 实际公开请求的状态、参数和请求头验证记录，客户端为 curl |
| `apifox/desktop-installation.json` | 官方安装包校验、两次安装尝试与实际崩溃信息 |

本项目目前是持久层实验，没有业务 HTTP 接口。按指导书使用公开接口完成 GET 练习，不把 Mapper 方法误写成 localhost REST 接口。

```http
GET https://postman-echo.com/get?course=java-framework&task=5
Accept: application/json
X-Lab-Task: experiment-1-task-5
```

2026-10-14 已实际发送以上请求，HTTP 状态为 200，响应 args 正确回显 course/task，headers 正确回显自定义请求头。集合中的 3 项断言已对捕获的实际响应执行通过，并验证错误的请求头值会使断言失败。这是请求与脚本验证，不是 ApiFox 客户端的导入、登录或用例保存记录。

### 在桌面端保存用例

桌面安装尚未完成。2026-10-14 核实 WinGet 官方来源包 `Ruihu.Apifox` 2.8.49 并通过 SHA256 校验，但 WinGet 静默安装和直接运行安装器均出现 `0xc0000005`。Windows 事件记录中的故障模块为安装器临时目录的 `System.dll`，尚不能据此确定根因。官方下载页的替代 ZIP 地址在本机返回 HTML，未继续把它当作可执行安装包使用。安装器副本保留在 `target/task5/Apifox-2.8.49.exe`，需要在本机解决安装故障后继续以下桌面步骤；本次没有创建 ApiFox 账号、项目或保存客户端用例。

1. 启动 ApiFox 桌面版，在本人账号或离线空间中建立学习工作空间，创建项目 `mybatis-lab`。安装来源使用 [ApiFox 官方下载页](https://docs.apifox.com/download)。
2. 进入项目的导入功能，选择 Postman 格式，导入 `task5.postman_collection.json`。也可以导入 OpenAPI 文件；两种文件选一种，避免重复创建接口。
3. 核对运行地址为 `https://postman-echo.com`，GET 路径为 `/get`。检查 Query 中 course/task 的值，Header 中 Accept 与 X-Lab-Task 的值。
4. 在后置操作中核对 3 项断言。若导入工具没有保留脚本，添加自定义脚本并粘贴 `task5-assertions.js`。ApiFox 的脚本兼容 Postman 语法，参见 [脚本官方说明](https://docs.apifox.com/scripts)。
5. 发送请求，检查 HTTP 200、JSON 响应和断言通过。在运行页选择“保存为用例”，命名为“实验一任务五 GET 参数及请求头回显”，同时保存响应。保存操作说明见 [接口调试用例](https://docs.apifox.com/doc-5220267)。
6. 在接口用例列表重新打开并执行一次，确认参数和请求头仍存在，再保存运行页及用例列表截图。导入接口文件不等于已在客户端保存用例。

无需在请求中配置 Authorization 或发送账号口令。公开接口临时不可用时记录实际网络错误并稍后重试，不用示例响应冒充现场运行。

## 步骤 5.3 代码审查与 AI 应用记录

以下为工具辅助核查结果，供本人逐项复核后写入实验报告；不能替代本人签署的人工审查。

| 核查项 | 项目证据 | 核查结论 |
| --- | --- | --- |
| 版本统一 | pom 由 MP 3.5.3.1 提供 MyBatis 3.5.10；任务四依赖树只有一个 MyBatis 核心版本 | MP 各模块版本一致，补齐 Spring Core 后 SQL 注入与查询通过 |
| 表与字段 | sql/init.sql、Employee 实体、XML 的 10 个字段及 MP 表注解 | staff_info 的 staff_id/entry_date 等列与属性映射一致，金额使用 BigDecimal、日期使用 LocalDate |
| 用户值绑定 | XML CRUD/动态 SQL 使用 `#{}`；条件构造器使用绑定值 | 未用 `${}` 拼接用户查询值；jdbc 配置中的 `${jdbc.*}` 是连接配置引用 |
| 映射完整 | 10 字段 resultMap 测试、真实插入查询、MP 字段映射测试 | 姓名、部门、性别、金额、岗位、日期、状态、邮箱、办公地点及主键有对应验证 |
| 主键回填 | XML useGeneratedKeys/keyProperty、MP IdType.AUTO、任务五对照演练 | 两种正确写法均能回填；缺失配置时通过数据库记录核实问题 |
| 动态 SQL 边界 | 空更新拒绝、空批量返回 0、null 参数拒绝、工资/状态 0 的更新测试 | 不生成无 SET 或无 VALUES 的 SQL，零值不会误判为未填写 |
| 分页与排序 | 已注册 MySQL 内部分页插件，工资降序后追加主键升序 | 实际测试 COUNT、偏移和页码边界，工资并列时顺序稳定 |
| 事务与资源 | try-with-resources、InnoDB 前置检查、测试 finally 回滚 | 演练和验收只操作自建记录，不依赖固定业务 ID |
| 命名和格式 | EmployeeMapper 与 EmployeePlusMapper 职责、故障代码集中在 test 目录 | 正式 XML 不残留故障；使用独立测试 namespace 避免覆盖正常 SQL |
| 配置口令 | jdbc-local.properties 被 Git 忽略，连接支持环境变量覆盖 | 请求练习及日志不包含数据库口令；本地配置不应公开提交 |

实际 AI 交互与工程修正可按以下记录核对。日期为本次工作日期，不应另编不存在的个人提示词或截图。

| 请求摘要 | 已完成的工作 | 需要本人复核的重点 |
| --- | --- | --- |
| 完善实验一任务一二三 | 数据保留式初始化、CRUD、动态 SQL、边界参数校验、回滚测试 | 表结构、事务边界与条件行为 |
| 完成任务四 | MP 实体与通用 Mapper、两种 Wrapper、分页及测试 | 依赖兼容、内部插件注册、业务状态与逻辑删除区别 |
| 完成任务五 | 三类故障及修复、真实异常记录、请求导入材料、断言及审查结论 | 真实日志与报告描述一致，ApiFox 用例需要在客户端保存 |
| 将项目改成员工表 | 员工实体与 emp 表、员工 Mapper/演示/测试、employee_db 连接和 IDEA 运行配置 | 字段映射、员工样例、启动入口与旧数据库数据保留 |

本次工程校验修正：将演练预期异常类型按实际运行记录为 ReflectionException；将故障限制在内存副本；给请求材料增加响应断言；区分公开 GET 验证与桌面端用例保存。本人复核后可把判断依据和自己的修正建议补入报告“AI 应用记录”部分。

## 验收与截图

完整回归命令：

```powershell
.\mvnw.cmd -Pdatabase-tests test
```

任务五新增 3 项 MySQL 故障与修复测试；2026-10-14 完整回归共 34 项全部通过，失败和错误均为 0。数据库测试会重新生成故障日志；仅运行默认的无数据库测试不会完成 5.1 演练。

实验报告建议使用三类故障及修复日志、JUnit 通过结果、ApiFox 请求头与响应、保存后的用例列表作为证据。账号登录、用例保存和个人截图需在本人桌面端核实，不用生成的示例图替代实际运行截图。
