-- ============================================================
-- 阶段 5：审批流程模块（请假 / 报销）
-- 请假申请 / 报销申请 / 审批记录（追加日志，无逻辑删除）
-- 说明：脚本可重复执行（幂等）。表不存在则创建。
-- 状态机值：0草稿 1待审批 2通过 3拒绝 4撤回
-- ============================================================

CREATE TABLE IF NOT EXISTS leave_request (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '申请人ID',
    leave_type  TINYINT      DEFAULT NULL COMMENT '请假类型：1年假 2事假 3病假',
    start_date  DATE         DEFAULT NULL COMMENT '开始日期',
    end_date    DATE         DEFAULT NULL COMMENT '结束日期',
    days        INT          DEFAULT NULL COMMENT '请假天数',
    reason      VARCHAR(500) DEFAULT NULL COMMENT '请假事由',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态机值：0草稿 1待审批 2通过 3拒绝 4撤回',
    approver_id BIGINT       DEFAULT NULL COMMENT '审批人ID',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (id),
    KEY idx_leave_user (user_id),
    KEY idx_leave_approver (approver_id)
) COMMENT = '请假申请';

CREATE TABLE IF NOT EXISTS reimbursement_request (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT         NOT NULL COMMENT '申请人ID',
    amount      DECIMAL(10,2)  DEFAULT NULL COMMENT '报销金额',
    category    VARCHAR(50)    DEFAULT NULL COMMENT '报销类别',
    reason      VARCHAR(500)   DEFAULT NULL COMMENT '报销事由',
    status      TINYINT        NOT NULL DEFAULT 0 COMMENT '状态机值：0草稿 1待审批 2通过 3拒绝 4撤回',
    approver_id BIGINT         DEFAULT NULL COMMENT '审批人ID',
    create_time DATETIME       DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME       DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (id),
    KEY idx_reimburse_user (user_id),
    KEY idx_reimburse_approver (approver_id)
) COMMENT = '报销申请';

-- 审批记录：审批流水的追加日志，不做逻辑删除
CREATE TABLE IF NOT EXISTS approval_record (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    business_type TINYINT      NOT NULL COMMENT '业务类型：1请假 2报销',
    business_id   BIGINT       NOT NULL COMMENT '业务单号',
    approver_id   BIGINT       DEFAULT NULL COMMENT '操作人ID（提交人/审批人）',
    action        TINYINT      NOT NULL COMMENT '动作：1提交 2通过 3拒绝 4撤回',
    comment       VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_record_business (business_type, business_id)
) COMMENT = '审批记录';
