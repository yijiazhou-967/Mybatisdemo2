-- 先执行 dept_emp_init.sql。本文件新增项目、项目成员关系和工牌，不覆盖员工/部门。
USE dept_emp_db;

CREATE TABLE IF NOT EXISTS project (
    project_id INT PRIMARY KEY AUTO_INCREMENT,
    project_code VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    start_date DATE,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目';

CREATE TABLE IF NOT EXISTS employee_project (
    project_id INT NOT NULL,
    empno INT NOT NULL,
    PRIMARY KEY (project_id, empno),
    KEY idx_membership_emp (empno),
    FOREIGN KEY (project_id) REFERENCES project(project_id),
    FOREIGN KEY (empno) REFERENCES emp(empno)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工参与项目（多对多）';

CREATE TABLE IF NOT EXISTS emp_card (
    empno INT PRIMARY KEY,
    card_number VARCHAR(40) NOT NULL UNIQUE,
    issued_on DATE NOT NULL,
    FOREIGN KEY (empno) REFERENCES emp(empno)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工工牌（一对一）';

INSERT INTO project (project_code, title, start_date)
SELECT seed.code, seed.title, seed.started
FROM (
    SELECT 'LAB-OFFICE' AS code, '办公协作平台' AS title, CAST('2026-03-01' AS DATE) AS started
    UNION ALL SELECT 'LAB-STOCK', '库存追踪系统', CAST('2026-04-15' AS DATE)
    UNION ALL SELECT 'LAB-EMPTY', '待启动项目', NULL
) seed
WHERE NOT EXISTS (SELECT 1 FROM project p WHERE p.project_code = seed.code);

-- 员工 1 同时参与两个项目，项目 LAB-OFFICE 有两位员工；LAB-EMPTY 没有成员。
INSERT INTO employee_project (project_id, empno)
SELECT p.project_id, e.empno
FROM (
    SELECT 'LAB-OFFICE' AS code, 1 AS empno
    UNION ALL SELECT 'LAB-OFFICE', 4
    UNION ALL SELECT 'LAB-STOCK', 1
    UNION ALL SELECT 'LAB-STOCK', 2
) seed
JOIN project p ON p.project_code = seed.code AND p.deleted = 0
JOIN emp e ON e.empno = seed.empno AND e.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM employee_project link WHERE link.project_id = p.project_id AND link.empno = e.empno
);

INSERT INTO emp_card (empno, card_number, issued_on)
SELECT e.empno, seed.card, seed.issued
FROM (
    SELECT 1 AS empno, 'LAB-CARD-001' AS card, CAST('2026-03-01' AS DATE) AS issued
    UNION ALL SELECT 4, 'LAB-CARD-004', CAST('2026-03-02' AS DATE)
) seed
JOIN emp e ON e.empno = seed.empno AND e.deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM emp_card c WHERE c.empno = e.empno);
