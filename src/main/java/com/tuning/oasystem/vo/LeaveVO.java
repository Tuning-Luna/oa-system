package com.tuning.oasystem.vo;

import com.tuning.oasystem.entity.LeaveRequest;
import com.tuning.oasystem.enums.ApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请信息返回
 */
@Data
@Schema(description = "请假申请信息")
public class LeaveVO {

    @Schema(description = "申请单ID")
    private Long id;

    @Schema(description = "申请人ID")
    private Long userId;

    @Schema(description = "申请人姓名")
    private String applicantName;

    @Schema(description = "请假类型：1年假 2事假 3病假")
    private Integer leaveType;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "请假天数")
    private Integer days;

    @Schema(description = "请假事由")
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

    public static LeaveVO from(LeaveRequest entity, String applicantName, String approverName) {
        LeaveVO vo = new LeaveVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setApplicantName(applicantName);
        vo.setLeaveType(entity.getLeaveType());
        vo.setStartDate(entity.getStartDate());
        vo.setEndDate(entity.getEndDate());
        vo.setDays(entity.getDays());
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
