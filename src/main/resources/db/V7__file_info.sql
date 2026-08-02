-- ============================================================
-- 阶段 7：文件管理模块
-- 文件信息表 file_info（物理文件存本地磁盘，表记录元数据）
-- 说明：脚本可重复执行（幂等）。表不存在则创建。
-- ============================================================

CREATE TABLE IF NOT EXISTS file_info (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    store_name    VARCHAR(255) NOT NULL COMMENT '存储文件名（UUID+扩展名）',
    path          VARCHAR(500) NOT NULL COMMENT '相对存储路径（upload-dir 下，如 20260802/uuid.png）',
    url           VARCHAR(500) DEFAULT NULL COMMENT '下载 URL',
    size          BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    content_type  VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
    uploader_id   BIGINT       NOT NULL COMMENT '上传人ID',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (id),
    KEY idx_uploader (uploader_id)
) COMMENT = '文件信息表';
