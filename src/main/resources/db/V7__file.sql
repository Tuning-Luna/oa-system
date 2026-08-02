-- ============================================================
-- 阶段 7：文件管理模块 —— 文件信息表
-- 说明：脚本可重复执行（幂等）。表不存在则创建。
-- store_name / path 均为存储相对路径（yyyy/MM/uuid.ext），磁盘操作以 path 为准。
-- ============================================================

CREATE TABLE IF NOT EXISTS file_info (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    original_name VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    store_name    VARCHAR(255) DEFAULT NULL COMMENT '存储文件名（相对路径）',
    path          VARCHAR(500) DEFAULT NULL COMMENT '存储相对路径',
    url           VARCHAR(500) DEFAULT NULL COMMENT '访问地址',
    size          BIGINT       DEFAULT NULL COMMENT '文件大小（字节）',
    content_type  VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
    uploader_id   BIGINT       DEFAULT NULL COMMENT '上传人ID',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (id),
    KEY idx_uploader (uploader_id)
) COMMENT = '文件信息表';
