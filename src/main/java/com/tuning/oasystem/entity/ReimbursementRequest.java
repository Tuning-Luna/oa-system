package com.tuning.oasystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报销申请实体（reimbursement_request）
 */
@Data
@TableName("reimbursement_request")
public class ReimbursementRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人ID */
    private Long userId;

    /** 报销金额 */
    private BigDecimal amount;

    /** 报销类别 */
    private String category;

    /** 报销事由 */
    private String reason;

    /** 状态机值：0草稿 1待审批 2通过 3拒绝 4撤回 */
    private Integer status;

    /** 审批人ID */
    private Long approverId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0否 1是 */
    @TableLogic
    private Integer deleted;
}
