-- 实验一自定义方案：企业员工档案，保留已有 emp 表。
CREATE DATABASE IF NOT EXISTS ssm_emp DEFAULT CHARACTER SET utf8mb4;
USE ssm_emp;

CREATE TABLE IF NOT EXISTS staff_info (
    staff_id        INT           PRIMARY KEY AUTO_INCREMENT COMMENT '员工档案编号',
    full_name       VARCHAR(60)   NOT NULL COMMENT '员工姓名',
    gender          CHAR(1)       DEFAULT '男' COMMENT '性别',
    department      VARCHAR(60)   NOT NULL COMMENT '所属部门',
    job_title       VARCHAR(60)   COMMENT '职位名称',
    monthly_salary  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '月薪（元）',
    entry_date      DATE          NULL COMMENT '入职日期',
    work_status     TINYINT       NOT NULL DEFAULT 1 COMMENT '工作状态：1在职 0离职',
    email           VARCHAR(100)  NULL COMMENT '工作邮箱',
    office_location VARCHAR(60)   NULL COMMENT '办公地点',
    UNIQUE KEY uk_staff_email (email),
    INDEX idx_staff_department (department)
) ENGINE = InnoDB COMMENT = '企业员工档案表';

-- 按姓名、部门、入职日期补齐样例；重复执行不覆盖已有档案。
INSERT INTO staff_info (full_name, gender, department, job_title, monthly_salary,
                        entry_date, work_status, email, office_location)
SELECT seed.full_name, seed.gender, seed.department, seed.job_title, seed.monthly_salary,
       seed.entry_date, seed.work_status, seed.email, seed.office_location
FROM (
    SELECT '陈嘉宁' AS full_name, '女' AS gender, '产品部' AS department, '产品经理' AS job_title,
           13500.00 AS monthly_salary, CAST('2023-09-12' AS DATE) AS entry_date, 1 AS work_status,
           'chenjianing@company.example' AS email, '上海办公室' AS office_location
    UNION ALL SELECT '刘子航', '男', '技术部', '后端开发工程师', 15000.00,
           CAST('2024-04-08' AS DATE), 1, 'liuzihang@company.example', '杭州研发中心'
    UNION ALL SELECT '孙雨桐', '女', '财务部', '财务分析师', 9800.00,
           CAST('2022-12-05' AS DATE), 1, 'sunyutong@company.example', '上海办公室'
    UNION ALL SELECT '周明远', '男', '运维部', '系统运维工程师', 11800.00,
           CAST('2025-02-17' AS DATE), 1, 'zhoumingyuan@company.example', '杭州研发中心'
    UNION ALL SELECT '许安然', '女', '设计部', '视觉设计师', 10500.00,
           CAST('2023-06-26' AS DATE), 0, 'xuanran@company.example', '成都办公室'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM staff_info AS existing
    WHERE existing.full_name = seed.full_name AND existing.department = seed.department
      AND existing.entry_date = seed.entry_date
);

SELECT staff_id, full_name, gender, department, job_title, monthly_salary,
       entry_date, work_status, email, office_location
FROM staff_info ORDER BY staff_id;
