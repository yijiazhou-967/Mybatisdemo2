-- 根据提供的 dept_emp.sql 初始化独立实验库；不执行原文件中的 DROP TABLE。
-- 补齐 Dept/Emp 的 deleted 字段。样例以原始编号识别，可重复执行。
CREATE DATABASE IF NOT EXISTS dept_emp_db DEFAULT CHARACTER SET utf8mb4;
USE dept_emp_db;

CREATE TABLE IF NOT EXISTS dept (
    deptno INT PRIMARY KEY AUTO_INCREMENT COMMENT '部门编号',
    dname VARCHAR(50) NOT NULL COMMENT '部门名称',
    loc VARCHAR(50) COMMENT '部门位置',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE IF NOT EXISTS emp (
    empno INT PRIMARY KEY AUTO_INCREMENT COMMENT '员工编号',
    ename VARCHAR(50) NOT NULL COMMENT '员工姓名',
    job VARCHAR(50) COMMENT '职位',
    mgr INT COMMENT '上级编号',
    hiredate DATE COMMENT '入职日期',
    sal DECIMAL(10,2) COMMENT '薪水',
    comm DECIMAL(10,2) COMMENT '佣金',
    deptno INT COMMENT '部门编号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    FOREIGN KEY (deptno) REFERENCES dept(deptno)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

INSERT INTO dept (deptno, dname, loc)
SELECT seed.deptno, seed.dname, seed.loc
FROM (
    SELECT 1 AS deptno, 'ACCOUNTING' AS dname, 'NEW YORK' AS loc
    UNION ALL SELECT 2, 'RESEARCH', 'DALLAS'
    UNION ALL SELECT 3, 'SALES', 'CHICAGO'
    UNION ALL SELECT 4, 'OPERATIONS', 'BOSTON'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM dept d WHERE d.deptno = seed.deptno);

INSERT INTO emp (empno, ename, job, mgr, hiredate, sal, comm, deptno)
SELECT seed.empno, seed.ename, seed.job, seed.mgr, seed.hiredate, seed.sal, seed.comm, seed.deptno
FROM (
    SELECT 1 AS empno, 'SMITH' AS ename, 'CLERK' AS job, 7902 AS mgr,
           CAST('1980-12-17' AS DATE) AS hiredate, 800.00 AS sal, NULL AS comm, 2 AS deptno
    UNION ALL SELECT 2, 'ALLEN', 'SALESMAN', 7698, CAST('1981-02-20' AS DATE), 1600.00, 300.00, 3
    UNION ALL SELECT 3, 'WARD', 'SALESMAN', 7698, CAST('1981-02-22' AS DATE), 1250.00, 500.00, 3
    UNION ALL SELECT 4, 'JONES', 'MANAGER', 7839, CAST('1981-04-02' AS DATE), 2975.00, NULL, 2
    UNION ALL SELECT 5, 'MARTIN', 'SALESMAN', 7698, CAST('1981-09-28' AS DATE), 1250.00, 1400.00, 3
    UNION ALL SELECT 6, 'BLAKE', 'MANAGER', 7839, CAST('1981-05-01' AS DATE), 2850.00, NULL, 3
    UNION ALL SELECT 7, 'CLARK', 'MANAGER', 7839, CAST('1981-06-09' AS DATE), 2450.00, NULL, 1
    UNION ALL SELECT 8, 'SCOTT', 'ANALYST', 7566, CAST('1987-04-19' AS DATE), 3000.00, NULL, 2
    UNION ALL SELECT 9, 'KING', 'PRESIDENT', NULL, CAST('1981-11-17' AS DATE), 5000.00, NULL, 1
    UNION ALL SELECT 10, 'TURNER', 'SALESMAN', 7698, CAST('1981-09-08' AS DATE), 1500.00, 0.00, 3
    UNION ALL SELECT 11, 'ADAMS', 'CLERK', 7788, CAST('1987-05-23' AS DATE), 1100.00, NULL, 2
    UNION ALL SELECT 12, 'JAMES', 'CLERK', 7698, CAST('1981-12-03' AS DATE), 950.00, NULL, 3
    UNION ALL SELECT 13, 'FORD', 'ANALYST', 7566, CAST('1981-12-03' AS DATE), 3000.00, NULL, 2
    UNION ALL SELECT 14, 'MILLER', 'CLERK', 7782, CAST('1982-01-23' AS DATE), 1300.00, NULL, 1
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM emp e WHERE e.empno = seed.empno);

-- 将原始教学样例改为中文，只转换匹配原值的字段，保留编号和自定义内容。
UPDATE dept d
JOIN (
    SELECT 1 AS id, 'ACCOUNTING' AS old_name, '财务部' AS new_name, 'NEW YORK' AS old_loc, '纽约' AS new_loc
    UNION ALL SELECT 2, 'RESEARCH', '研发部', 'DALLAS', '达拉斯'
    UNION ALL SELECT 3, 'SALES', '销售部', 'CHICAGO', '芝加哥'
    UNION ALL SELECT 4, 'OPERATIONS', '运营部', 'BOSTON', '波士顿'
) names ON names.id = d.deptno
SET d.dname = CASE WHEN d.dname = names.old_name THEN names.new_name ELSE d.dname END,
    d.loc = CASE WHEN d.loc = names.old_loc THEN names.new_loc ELSE d.loc END;

UPDATE emp e
JOIN (
    SELECT 1 AS id, 'SMITH' AS old_name, '陈明' AS new_name
    UNION ALL SELECT 2, 'ALLEN', '李华'
    UNION ALL SELECT 3, 'WARD', '王磊'
    UNION ALL SELECT 4, 'JONES', '周杰'
    UNION ALL SELECT 5, 'MARTIN', '刘洋'
    UNION ALL SELECT 6, 'BLAKE', '赵伟'
    UNION ALL SELECT 7, 'CLARK', '孙宁'
    UNION ALL SELECT 8, 'SCOTT', '吴晨'
    UNION ALL SELECT 9, 'KING', '郑峰'
    UNION ALL SELECT 10, 'TURNER', '冯宇'
    UNION ALL SELECT 11, 'ADAMS', '何涛'
    UNION ALL SELECT 12, 'JAMES', '高远'
    UNION ALL SELECT 13, 'FORD', '林硕'
    UNION ALL SELECT 14, 'MILLER', '徐帆'
) names ON names.id = e.empno
SET e.ename = CASE WHEN e.ename = names.old_name THEN names.new_name ELSE e.ename END,
    e.job = CASE e.job WHEN 'CLERK' THEN '行政专员' WHEN 'SALESMAN' THEN '销售专员'
                      WHEN 'MANAGER' THEN '部门经理' WHEN 'ANALYST' THEN '数据分析师'
                      WHEN 'PRESIDENT' THEN '总经理' ELSE e.job END;

SELECT deptno, dname, loc, deleted FROM dept ORDER BY deptno;
SELECT empno, ename, job, deptno, sal, deleted FROM emp ORDER BY empno;
