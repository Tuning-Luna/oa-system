package com.tuning.oasystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请实体（leave_request）
 */
@Data
@TableName("leave_request")
public class LeaveRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人ID */
    private Long userId;

    /** 请假类型：1年假 2事假 3病假 */
    private Integer leaveType;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 请假天数 */
    private Integer days;

    /** 请假事由 */
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
