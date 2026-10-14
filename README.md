# 实验一 · 任务一、二、三：MyBatis 环境搭建、CRUD 与动态 SQL

课程：《JAVA框架技术（一）》（AI 赋能版）。沿用本项目的音乐专辑业务，数据库为 `music_store`、数据表为 `album`，对应指导书的员工示例。任务四的 MyBatis-Plus 与分页不在本次范围。

## 任务与文件对应

| 指导书步骤 | 实现或操作 | 文件 |
| --- | --- | --- |
| 1.1 数据库准备 | 建库、建表、索引、5 条样例，重复执行保留已有数据 | `sql/init.sql` |
| 1.2 Maven 环境配置 | 本地仓库与阿里云镜像示例、Maven Wrapper | `docs/maven-settings-example.xml`、`.mvn/wrapper/` |
| 1.3 Git 初始化 | 项目已有 Git 仓库及任务 1、任务 2/3 的历史提交 | `git log --oneline` |
| 1.4 AI 插件 | 在 IDEA 安装、登录并保留实际交互记录 | 需本人在 IDEA 完成 |
| 1.5 Maven 项目与依赖 | Java 11、MyBatis、MySQL、日志、JUnit | `pom.xml` |
| 2.1 全局配置 | 数据源、事务、驼峰映射、别名、Mapper 扫描 | `src/main/resources/mybatis-config.xml` |
| 2.2 实体类 | 主键、字符串、金额、日期、状态与存取方法 | `src/main/java/com/music/entity/Album.java` |
| 2.3 Mapper 与映射 | CRUD、显式 resultMap、自增主键回填 | `AlbumMapper.java`、`AlbumMapper.xml` |
| 2.4 第一个程序 | 共用 SqlSessionFactory、会话关闭、三种演示模式 | `MusicStoreDemo.java`、`MyBatisUtil.java` |
| 2.5 SQL 日志 | 输出 SQL、占位参数、结果条数 | `src/main/resources/logback.xml` |
| 2.6 JUnit | CRUD、字段映射、删除、事务回滚 | `AlbumMapperTest.java` |
| 3.1 多条件查询 | `if/where` 与等效 `trim`，条件为空查询全部 | `selectByCondition`、`selectByConditionWithTrim` |
| 3.2 动态更新 | `set`，仅更新非空字段，拒绝空更新 | `updateSelective` |
| 3.3 批量操作 | `foreach`、`@Param`、空列表处理 | `insertBatch`、`deleteBatch` |
| 3.4 分支查询 | 标题优先，其次歌手，无条件查全部 | `selectSmart` |

## 任务一：环境搭建与项目初始化

### 1.1 数据库准备

使用 JDK 11 或 17、MySQL 8.0、IntelliJ IDEA；Maven Wrapper 固定使用 Maven 3.9.6。实体使用 `LocalDate` 表示日期、`BigDecimal` 表示金额。本项目保留已有 JUnit 5 与 SLF4J/Logback，不改成指导书示例的 JUnit 4/Log4j；它们承担相同的测试和日志功能。

在项目根目录启动 MySQL 客户端，随后在 **MySQL 命令行**中导入脚本。此方式也适用于 Windows PowerShell：

```powershell
mysql --default-character-set=utf8mb4 -u root -p
```

```sql
source sql/init.sql;
USE music_store;
SHOW CREATE TABLE album;
SHOW INDEX FROM album;
SELECT * FROM album ORDER BY album_id;
```

| 字段 | Java 类型 | 数据库类型 | 说明 |
| --- | --- | --- | --- |
| `album_id` | Integer | INT，自增主键 | 专辑编号 |
| `title` | String | VARCHAR(100)，非空 | 名称 |
| `artist` | String | VARCHAR(60)，非空 | 歌手，普通索引 |
| `genre` | String | VARCHAR(30)，非空 | 风格 |
| `price` | BigDecimal | DECIMAL(10,2)，非空 | 售价 |
| `stock` | Integer | INT，非空，默认 0 | 库存 |
| `release_date` | LocalDate | DATE，可空 | 发行日期 |
| `status` | Integer | TINYINT，非空，默认 1 | 1 在售、0 下架 |

初始化脚本使用 `IF NOT EXISTS`，并按标题、歌手和发行日期补齐缺失的样例，不删除库、不覆盖已有专辑。新库应有 5 条样例；已有库可能有更多记录。表使用 InnoDB，才能支持事务回滚。已有表结构不由脚本自动迁移。

### 1.2 Maven 配置

把 `docs/maven-settings-example.xml` 中的镜像与本地仓库设置合并到自己的 `settings.xml`，填写实际仓库路径。在 IDEA 的 `Settings → Build Tools → Maven` 中选择 Maven Wrapper，并指定同一份用户配置。不要直接用示例覆盖现有全局设置。

```powershell
.\mvnw.cmd -version
.\mvnw.cmd test
```

首次执行需要联网下载依赖。只在依赖完整缓存、仓库 ID 一致时使用 `-o`。本机独立 Maven 与 Wrapper 的版本、settings.xml 可能不同，优先统一使用 Wrapper。`pom.xml` 固定资源、编译、测试插件版本，避免切换 Maven 后改变插件选择。

### 1.3 Git 与 1.4 AI 插件

项目已初始化 Git，无需重复执行 `git init`。查看与提交本人核对后的改动：

```powershell
git status --short
git log --oneline
git diff
git add README.md pom.xml .gitignore sql src docs
git commit -m "feat: 完善实验一任务一二三"
```

在 IDEA 插件市场安装通义灵码、GitHub Copilot 或 CodeGeeX，完成本人账号登录，开启补全并人工核对生成内容。项目文件不能证明插件已安装或账号已激活，需要本人检查。保留实际提示词、修改记录与运行截图。

### 1.5 依赖与数据库连接

| 依赖 | 版本 |
| --- | --- |
| MyBatis | 3.5.16 |
| MySQL Connector/J | 8.4.0 |
| SLF4J | 2.0.13 |
| Logback | 1.5.6 |
| JUnit Jupiter | 5.10.2 |

`jdbc.properties` 仅保存示例值。原开发配置已保存在 `src/main/resources/jdbc-local.properties`，该文件被 Git 忽略。新检出项目可以复制 `jdbc.properties` 为 `jdbc-local.properties`，再填写本机账号与密码。

也可通过 IDEA Run Configuration 设置 `MUSIC_DB_URL`、`MUSIC_DB_USERNAME`、`MUSIC_DB_PASSWORD` 环境变量。覆盖优先级从高到低：JVM 系统属性 `jdbc.*` → `MUSIC_DB_*` 环境变量 → `jdbc-local.properties` → `jdbc.properties`。不要在截图或提交中展示口令。资源文件按 UTF-8 保存。

本次实际运行遇到 `Public Key Retrieval is not allowed`，已在 localhost 实验连接中加入 `allowPublicKeyRetrieval=true`，允许驱动取得服务器 RSA 公钥。该连接配置用于本机实验；远程环境应配置 TLS 与服务器身份校验。参数含义见 [MySQL Connector/J 官方说明](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html)。

## 任务二：MyBatis 配置与基础 CRUD

`mybatis-config.xml` 加载连接属性，使用 JDBC 事务与连接池，注册实体别名及 Mapper 包。接口与 XML 同名，并保持相同包路径；XML 的 namespace 为 `com.music.mapper.AlbumMapper`。

`AlbumMapper.xml` 中的 `albumMap` 显式映射 8 个字段，特别是 `album_id → albumId`、`release_date → releaseDate`。新增操作使用 `useGeneratedKeys` 回填主键；按 ID 查询与删除显式使用 `@Param`。可空日期绑定 `jdbcType=DATE`。

在 IDEA 中运行 `com.music.MusicStoreDemo`，Program arguments 选择：

| 参数 | 演示内容 |
| --- | --- |
| 无参数或 `list` | 查询全部，输出专辑及 SQL 日志 |
| `crud` | 插入、主键回填、查询、整体更新、删除 |
| `dynamic` | 批量插入、条件查询、trim、局部更新、分支查询、批量删除 |

演示程序使用独立临时歌手标记，结束时回滚事务。正式业务需要保存修改时，在成功路径显式调用 `session.commit()`；异常路径回滚，始终关闭会话。SqlSessionFactory 共用，SqlSession 不跨线程共享。

日志可见 `Preparing`（SQL）、`Parameters`（实参）、`Total`（查询条数）。增删改还可通过 Mapper 返回的影响行数验证。

## 任务三：动态 SQL 与批量操作

- `selectByCondition`：标题与歌手模糊匹配，风格与状态精确匹配；`where` 清除首个多余 AND。`selectByConditionWithTrim` 使用相同条件片段，验证 `trim` 的等效效果。
- `updateSelective`：先验证正整数主键与至少一个更新字段，再通过 `set` 拼接非空字段。库存和状态为 0 时正常更新。null 表示不修改；要把发行日期置空，使用整体更新 `update`。
- `insertBatch`、`deleteBatch`：空列表返回 0；null 列表、null 元素或无效删除主键抛出 `IllegalArgumentException`。校验通过后才调用 `*Rows` 映射入口，业务代码应调用带校验的方法。
- `selectSmart`：标题非空时只按标题查；否则歌手非空时只按歌手查；两者都没有时查全部，包括下架专辑。它与 `if` 条件叠加的行为不同。

所有用户值均使用 `#{}` 参数绑定；LIKE 中的 `%`、`_` 按 MySQL 通配符规则匹配。

## 测试与验收

默认运行 9 项 SQL/参数测试，无需启动 MySQL：

```powershell
.\mvnw.cmd test
```

数据库初始化并配置连接后，运行全部测试（9 项 SQL 测试 + 11 项 MySQL 测试）：

```powershell
.\mvnw.cmd -Pdatabase-tests test
```

MySQL 测试先核实 album 使用 InnoDB，各用例自行插入独立标记数据，结束时回滚，不依赖固定样例或删除别人的数据。回滚不恢复 MySQL 自增序号，因此 ID 出现间隔是正常现象。IDEA 直接运行 `AlbumMapperTest` 也会连接数据库。

测试报告位于 `target/surefire-reports/`。默认测试通过只说明配置和 SQL 生成正常；数据库测试通过才说明连接、实际 SQL 执行与结果映射已验证。

2026-10-14 本机验收：20 项测试全部通过；`crud`、`dynamic` 演示均正常。初始化脚本已在独立临时数据库中连续执行 3 次，验证样例不重复、自建记录保留、修改过的样例价格不被覆盖，验收后删除了该临时数据库。

## 排错与实验记录

| 现象 | 排查方法 |
| --- | --- |
| Connection refused / Communications link failure | 检查 MySQL 服务、端口和连接 URL |
| Access denied | 检查本地文件或环境变量中的账号与口令 |
| Public Key Retrieval is not allowed | 本机实验核对 `allowPublicKeyRetrieval=true`，远程环境配置 TLS |
| Unknown database / Table doesn't exist | 在 MySQL 客户端执行初始化脚本，核对连接的数据库 |
| Invalid bound statement / Parameter not found | 核对 namespace、接口方法、XML id、资源路径及 `@Param` 名称 |
| 离线依赖 present but unavailable | 核对 Maven 配置与缓存来源仓库 ID；依赖未缓存时联网运行 |

本次核对发现并修正了空更新/空批量生成无效 SQL、测试提前提交导致失败后遗留数据、分支默认只查在售与指导书不一致、初始化直接删库等问题。记录本人实际出现的报错及解决过程，不将这些代码检查发现伪写成个人运行经历。

本机离线验收还遇到 Surefire 依赖来自 `alimaven`、Wrapper 使用 `central` 导致缓存不可用的问题。先使用临时配置登记两个缓存来源完成离线验证，随后联网补齐缓存；标准命令 `.\mvnw.cmd -Pdatabase-tests test` 也已验证通过，未改动全局 Maven 配置。

实验报告可据此整理任务步骤，但仍需补充本人环境版本、AI 插件配置、真实交互记录和截图：初始化查询、SQL 与参数日志、CRUD/动态 SQL 演示、JUnit 测试结果。插件登录、截图与报告签名需本人完成。
