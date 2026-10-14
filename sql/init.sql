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

-- 保留已有库和数据；可重复执行。已有表结构不由本脚本自动迁移。
CREATE DATABASE IF NOT EXISTS music_store DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE music_store;

-- ----------------------------------------------------------------------------
-- 专辑表 album：记录一张音乐专辑的核心信息
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS album (
    album_id     INT           PRIMARY KEY AUTO_INCREMENT COMMENT '专辑编号（主键，自增）',
    title        VARCHAR(100)  NOT NULL                COMMENT '专辑名称',
    artist       VARCHAR(60)   NOT NULL                COMMENT '歌手 / 乐队',
    genre        VARCHAR(30)   NOT NULL                COMMENT '音乐风格',
    price        DECIMAL(10,2) NOT NULL                COMMENT '售价（元）',
    stock        INT           NOT NULL DEFAULT 0      COMMENT '库存数量',
    release_date DATE          NULL                    COMMENT '发行日期',
    status       TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1 在售 / 0 下架',
    INDEX idx_album_artist (artist)
) ENGINE = InnoDB COMMENT = '音乐专辑表';

-- ----------------------------------------------------------------------------
-- 初始化测试数据（覆盖多种风格与状态，便于验证增删改查与多条件筛选）
-- ----------------------------------------------------------------------------
-- 按标题、歌手和发行日期判断样例是否存在，避免重复插入或覆盖已修改记录。
INSERT INTO album (title, artist, genre, price, stock, release_date, status)
SELECT seed.title, seed.artist, seed.genre, seed.price, seed.stock, seed.release_date, seed.status
FROM (
    SELECT '晴天' AS title, '周杰伦' AS artist, '流行' AS genre,
           35.00 AS price, 200 AS stock, CAST('2003-07-31' AS DATE) AS release_date, 1 AS status
    UNION ALL SELECT 'Faded', 'Alan Walker', '电子', 45.00, 150, CAST('2015-12-04' AS DATE), 1
    UNION ALL SELECT 'Bohemian Rhapsody', 'Queen', '摇滚', 55.00, 120, CAST('1975-10-31' AS DATE), 1
    UNION ALL SELECT 'Thriller', 'Michael Jackson', '流行', 50.00, 90, CAST('1982-11-30' AS DATE), 1
    UNION ALL SELECT '花海', '周杰伦', '流行', 38.00, 300, CAST('2008-10-15' AS DATE), 0
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM album AS existing
    WHERE existing.title = seed.title AND existing.artist = seed.artist
      AND existing.release_date = seed.release_date
);

SELECT album_id, title, artist, genre, price, stock, release_date, status FROM album ORDER BY album_id;
