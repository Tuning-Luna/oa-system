package com.tuning.oasystem.vo;

import com.tuning.oasystem.entity.ApprovalRecord;
import com.tuning.oasystem.enums.ApprovalAction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批记录返回
 */
@Data
@Schema(description = "审批记录")
public class ApprovalRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "业务类型：1请假 2报销")
    private Integer businessType;

    @Schema(description = "业务单号")
    private Long businessId;

    @Schema(description = "操作人ID")
    private Long approverId;

    @Schema(description = "操作人姓名")
    private String approverName;

    @Schema(description = "动作值：1提交 2通过 3拒绝 4撤回")
    private Integer action;

    @Schema(description = "动作描述")
    private String actionDesc;

    @Schema(description = "审批意见")
    private String comment;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;

    public static ApprovalRecordVO from(ApprovalRecord entity, String approverName) {
        ApprovalRecordVO vo = new ApprovalRecordVO();
        vo.setId(entity.getId());
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessId(entity.getBusinessId());
        vo.setApproverId(entity.getApproverId());
        vo.setApproverName(approverName);
        vo.setAction(entity.getAction());
        vo.setActionDesc(valueOf(entity.getAction()).getDesc());
        vo.setComment(entity.getComment());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 动作值 → 枚举（避免在 VO 中直接依赖异常处理）
     */
    private static ApprovalAction valueOf(Integer value) {
        for (ApprovalAction action : ApprovalAction.values()) {
            if (action.getValue().equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("未知审批动作: " + value);
    }
}
