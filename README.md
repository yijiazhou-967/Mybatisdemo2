# 实验一 · 任务一：MyBatis 开发环境搭建与项目初始化

> 课程：《JAVA框架技术（一）》（AI 赋能版）
> 业务载体：**在线音乐专辑商城（music_store）**，核心表为 `album`
> 说明：本实验以「音乐专辑」完全替代教材中的「员工(emp)」示例，所有表结构与代码均为独立设计。

本任务聚焦**持久层开发环境从零搭建**，对应指导书“任务 1（2 学时）”，共 5 个步骤：

| 步骤 | 内容 | 产出 |
| ---- | ---- | ---- |
| 1.1 | 数据库准备 | `sql/init.sql` |
| 1.2 | Maven 加速配置 | `docs/maven-settings-example.xml` |
| 1.3 | Git 版本控制初始化 | 本地提交历史 |
| 1.4 | AI 编程插件安装与配置 | IDEA 插件（人工完成） |
| 1.5 | 创建 Maven 项目并引入依赖 | `pom.xml` |

---

## 步骤 1.1 数据库准备

在 MySQL 8.0 客户端中执行项目根目录下的 `sql/init.sql`：

```bash
mysql -u root -p < sql/init.sql
```

脚本完成三件事：

1. 创建数据库 `music_store`（utf8mb4 字符集）；
2. 创建专辑表 `album`，字段如下：

| 字段 | 类型 | 约束 | 说明 |
| ---- | ---- | ---- | ---- |
| `album_id` | INT | 主键、自增 | 专辑编号 |
| `title` | VARCHAR(100) | 非空 | 专辑名称 |
| `artist` | VARCHAR(60) | 非空 | 歌手/乐队 |
| `genre` | VARCHAR(30) | 非空 | 音乐风格 |
| `price` | DECIMAL(10,2) | 非空 | 售价 |
| `stock` | INT | 非空、默认 0 | 库存数量 |
| `release_date` | DATE | 可空 | 发行日期 |
| `status` | TINYINT | 非空、默认 1 | 1 在售 / 0 下架 |

3. 为 `artist` 建立普通索引，并插入 5 条覆盖不同风格与状态的测试数据。

验证：`SELECT * FROM album;` 能返回 5 行即代表数据库准备成功。

---

## 步骤 1.2 Maven 加速配置

为提升依赖下载速度，在 Maven 的 `settings.xml` 中配置**本地仓库路径**与**阿里云镜像**。
完整示例见 `docs/maven-settings-example.xml`，核心改动两点：

- `<localRepository>`：指向本地仓库目录；
- `<mirrors>`：将 `*` 全部镜像到 `https://maven.aliyun.com/repository/public`。

在 IDEA 中通过 `File → Settings → Build Tools → Maven` 指定 Maven 主目录、
`settings.xml` 与本地仓库路径，使 IDEA 与命令行共用同一套配置。

---

## 步骤 1.3 Git 版本控制初始化

1. 配置全局身份：

   ```bash
   git config --global user.name  "你的姓名拼音"
   git config --global user.email "你的学号@school.edu.cn"
   ```

2. 在项目根目录启用版本控制并完成首次提交：

   ```bash
   git init
   git add .
   git commit -m "init: mybatis 环境搭建（音乐专辑商城）"
   ```

> 建议每个任务完成节点各提交一次，保留开发轨迹。

---

## 步骤 1.4 AI 编程插件安装与配置

在 IDEA 插件市场安装任一 AI 插件（通义灵码 / GitHub Copilot / CodeGeeX），完成登录与工作空间绑定：

- 开启代码补全与行内提示；
- 对 AI 生成的代码设置人工审查流程，不自动应用未审查代码；
- **严禁**把个人隐私、真实账号口令发送给 AI 工具。

---

## 步骤 1.5 创建 Maven 项目并引入依赖

本项目已基于 Maven 完成初始化，核心依赖见 `pom.xml`：

| 依赖 | 版本 | 用途 |
| ---- | ---- | ---- |
| `org.mybatis:mybatis` | 3.5.16 | 持久层框架 |
| `com.mysql:mysql-connector-j` | 8.4.0 | MySQL 驱动（runtime） |
| `org.slf4j:slf4j-api` | 2.0.13 | 日志门面 |
| `ch.qos.logback:logback-classic` | 1.5.6 | 日志实现（输出 SQL 日志） |
| `org.junit.jupiter:junit-jupiter` | 5.10.2 | 单元测试（test） |

依赖版本统一收敛在 `<properties>` 中集中管理，编译目标字节码为 JDK 11（本机 JDK 17 向下兼容）。

验证依赖无冲突：

```bash
mvn clean compile
```

控制台无报错、依赖正常下载即代表环境搭建完成，可进入任务二（编写 `mybatis-config.xml`、
实体类与 Mapper，完成基础 CRUD）。
