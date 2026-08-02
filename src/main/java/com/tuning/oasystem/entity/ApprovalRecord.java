package com.tuning.oasystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批记录实体（approval_record）——审批流水追加日志，无逻辑删除
 */
@Data
@TableName("approval_record")
public class ApprovalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型：1请假 2报销 */
    private Integer businessType;

    /** 业务单号 */
    private Long businessId;

    /** 操作人ID（提交人 / 审批人） */
    private Long approverId;

    /** 动作：1提交 2通过 3拒绝 4撤回 */
    private Integer action;

    /** 审批意见 */
    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
