-- ============================================================
-- 阶段 2：用户模块
-- 用户表 sys_user + 初始管理员账号（admin）
-- 说明：脚本可重复执行（幂等）。表不存在则创建；admin 已存在则不重复插入。
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) COMMENT = '用户表';

-- 初始管理员账号（密码：Admin@123456，BCrypt 密文）
INSERT INTO sys_user (username, password, nickname, email, status, create_time, update_time, deleted)
VALUES ('admin', '$2a$10$d4RHUelT9g8KZoidi6pTAO2m.VGr5Dc0Acc6u.d7ga1pSB3TZLEHO', '系统管理员', 'admin@oa.local', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE update_time = NOW();
