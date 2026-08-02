package com.tuning.oasystem.service;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.dto.ApprovalQuery;
import com.tuning.oasystem.dto.LeaveSubmitRequest;
import com.tuning.oasystem.vo.LeaveVO;

/**
 * 请假审批服务
 */
public interface LeaveService {

    /** 提交请假申请（DRAFT → PENDING，落一条"提交"记录） */
    LeaveVO submit(Long applicantId, LeaveSubmitRequest request);

    /** 审批通过（仅指定审批人，PENDING → APPROVED） */
    void approve(Long operatorId, Long id, String comment);

    /** 审批拒绝（仅指定审批人，PENDING → REJECTED） */
    void reject(Long operatorId, Long id, String comment);

    /** 撤回（仅申请人，DRAFT/PENDING → CANCELLED） */
    void cancel(Long operatorId, Long id);

    /** 申请详情（申请人或审批人可查看） */
    LeaveVO detail(Long operatorId, Long id);

    /** 我的申请列表（分页，可按状态筛选） */
    PageResult<LeaveVO> myPage(Long userId, ApprovalQuery query);

    /** 待我审批列表（分页） */
    PageResult<LeaveVO> pendingPage(Long userId, ApprovalQuery query);
}
