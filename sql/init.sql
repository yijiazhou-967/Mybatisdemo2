-- ============================================================================
-- 《JAVA框架技术（一）》 实验一 · 任务一 · 数据库初始化脚本
-- ----------------------------------------------------------------------------
-- 数据库：music_store   （在线音乐专辑商城）
-- 业务载体：album（音乐专辑）表
--
-- 说明：本实验完全以「音乐专辑」为业务载体，独立于教材中的员工(emp)表设计，
--       用于练习 MyBatis / MyBatis-Plus 的持久层开发与基础 CRUD。
-- 执行方式：在 MySQL 8.0 客户端中逐条执行，或 `source 本文件路径` 一键导入。
-- ============================================================================

-- 若已存在同名库则先删除，保证脚本可重复执行
DROP DATABASE IF EXISTS music_store;

-- 创建实验数据库（utf8mb4 字符集，支持中文与 emoji）
CREATE DATABASE music_store DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE music_store;

-- ----------------------------------------------------------------------------
-- 专辑表 album：记录一张音乐专辑的核心信息
-- ----------------------------------------------------------------------------
CREATE TABLE album (
    album_id     INT           PRIMARY KEY AUTO_INCREMENT COMMENT '专辑编号（主键，自增）',
    title        VARCHAR(100)  NOT NULL                COMMENT '专辑名称',
    artist       VARCHAR(60)   NOT NULL                COMMENT '歌手 / 乐队',
    genre        VARCHAR(30)   NOT NULL                COMMENT '音乐风格',
    price        DECIMAL(10,2) NOT NULL                COMMENT '售价（元）',
    stock        INT           NOT NULL DEFAULT 0      COMMENT '库存数量',
    release_date DATE          NULL                    COMMENT '发行日期',
    status       TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1 在售 / 0 下架'
) COMMENT = '音乐专辑表';

-- 为「歌手」字段建立普通索引，便于后续按歌手做条件查询
CREATE INDEX idx_album_artist ON album (artist);

-- ----------------------------------------------------------------------------
-- 初始化测试数据（覆盖多种风格与状态，便于验证增删改查与多条件筛选）
-- ----------------------------------------------------------------------------
INSERT INTO album (title, artist, genre, price, stock, release_date, status) VALUES
('晴天',             '周杰伦',          '流行', 35.00, 200, '2003-07-31', 1),
('Faded',            'Alan Walker',     '电子', 45.00, 150, '2015-12-04', 1),
('Bohemian Rhapsody','Queen',           '摇滚', 55.00, 120, '1975-10-31', 1),
('Thriller',         'Michael Jackson', '流行', 50.00,  90, '1982-11-30', 1),
('花海',             '周杰伦',          '流行', 38.00, 300, '2008-10-15', 0);
