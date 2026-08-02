package com.tuning.oasystem.vo;

import com.tuning.oasystem.entity.ReimbursementRequest;
import com.tuning.oasystem.enums.ApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报销申请信息返回
 */
@Data
@Schema(description = "报销申请信息")
public class ReimburseVO {

    @Schema(description = "申请单ID")
    private Long id;

    @Schema(description = "申请人ID")
    private Long userId;

    @Schema(description = "申请人姓名")
    private String applicantName;

    @Schema(description = "报销金额")
    private BigDecimal amount;

    @Schema(description = "报销类别")
    private String category;

    @Schema(description = "报销事由")
    private String reason;

    @Schema(description = "状态值")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批人姓名")
    private String approverName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public static ReimburseVO from(ReimbursementRequest entity, String applicantName, String approverName) {
        ReimburseVO vo = new ReimburseVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setApplicantName(applicantName);
        vo.setAmount(entity.getAmount());
        vo.setCategory(entity.getCategory());
        vo.setReason(entity.getReason());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(ApprovalStatus.of(entity.getStatus()).getDesc());
        vo.setApproverId(entity.getApproverId());
        vo.setApproverName(approverName);
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
