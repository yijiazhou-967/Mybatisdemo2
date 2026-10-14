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

-- 在插入样例前转换旧编码，确保原项目编号和成员关系保持一致。
UPDATE project p
JOIN (
    SELECT 'LAB-OFFICE' AS old_code, '协作项目' AS new_code
    UNION ALL SELECT 'LAB-STOCK', '库存项目'
    UNION ALL SELECT 'LAB-EMPTY', '待启动项目'
) names ON p.project_code = names.old_code
LEFT JOIN project occupied ON occupied.project_code = names.new_code
SET p.project_code = names.new_code
WHERE occupied.project_id IS NULL;

UPDATE emp_card c
JOIN (
    SELECT 1 AS empno, 'LAB-CARD-001' AS old_number, '工牌-001' AS new_number
    UNION ALL SELECT 4, 'LAB-CARD-004', '工牌-004'
) names ON c.empno = names.empno AND c.card_number = names.old_number
LEFT JOIN emp_card occupied ON occupied.card_number = names.new_number
SET c.card_number = names.new_number
WHERE occupied.empno IS NULL;

INSERT INTO project (project_code, title, start_date)
SELECT seed.code, seed.title, seed.started
FROM (
    SELECT '协作项目' AS code, '办公协作平台' AS title, CAST('2026-03-01' AS DATE) AS started
    UNION ALL SELECT '库存项目', '库存追踪系统', CAST('2026-04-15' AS DATE)
    UNION ALL SELECT '待启动项目', '待启动项目', NULL
) seed
WHERE NOT EXISTS (SELECT 1 FROM project p WHERE p.project_code = seed.code);

-- 员工 1 同时参与两个项目，协作项目有两位员工；待启动项目没有成员。
INSERT INTO employee_project (project_id, empno)
SELECT p.project_id, e.empno
FROM (
    SELECT '协作项目' AS code, 1 AS empno
    UNION ALL SELECT '协作项目', 4
    UNION ALL SELECT '库存项目', 1
    UNION ALL SELECT '库存项目', 2
) seed
JOIN project p ON p.project_code = seed.code AND p.deleted = 0
JOIN emp e ON e.empno = seed.empno AND e.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM employee_project link WHERE link.project_id = p.project_id AND link.empno = e.empno
);

INSERT INTO emp_card (empno, card_number, issued_on)
SELECT e.empno, seed.card, seed.issued
FROM (
    SELECT 1 AS empno, '工牌-001' AS card, CAST('2026-03-01' AS DATE) AS issued
    UNION ALL SELECT 4, '工牌-004', CAST('2026-03-02' AS DATE)
) seed
JOIN emp e ON e.empno = seed.empno AND e.deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM emp_card c WHERE c.empno = e.empno);
