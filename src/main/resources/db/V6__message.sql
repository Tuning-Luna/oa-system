-- ============================================================
-- 阶段 6：RabbitMQ 消息模块 —— 系统通知表
-- 说明：表名用 sys_message（message 为 MySQL 保留字，且符合 sys_ 前缀约定）。
-- 通知为追加型数据，无逻辑删除/更新时间字段。脚本可重复执行（幂等）。
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    receiver_id BIGINT       NOT NULL COMMENT '接收人ID',
    type        VARCHAR(20)  NOT NULL DEFAULT 'APPROVAL' COMMENT '通知类型：APPROVAL审批通知',
    title       VARCHAR(100) DEFAULT NULL COMMENT '标题',
    content     VARCHAR(500) DEFAULT NULL COMMENT '内容',
    read_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_receiver (receiver_id, read_flag)
) COMMENT = '系统通知表';
