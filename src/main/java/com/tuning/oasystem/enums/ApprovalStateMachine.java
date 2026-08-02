package com.tuning.oasystem.enums;

import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.exception.BusinessException;

import java.util.Map;

/**
 * 审批状态机：集中定义「动作 × 当前状态 → 新状态」的合法流转表（不可变）。
 * <p>
 * 合法流转：
 * <pre>
 * 提交 submit   : DRAFT → PENDING
 * 通过 approve  : PENDING → APPROVED
 * 拒绝 reject   : PENDING → REJECTED
 * 撤回 cancel   : DRAFT / PENDING → CANCELLED
 * </pre>
 * 非法流转（如对已通过的单再次通过）直接抛 {@link BusinessException}。
 */
public final class ApprovalStateMachine {

    private ApprovalStateMachine() {
    }

    /** 不可变流转表：动作 → (当前状态 → 新状态) */
    private static final Map<ApprovalAction, Map<ApprovalStatus, ApprovalStatus>> TRANSITIONS = Map.of(
            ApprovalAction.SUBMIT, Map.of(ApprovalStatus.DRAFT, ApprovalStatus.PENDING),
            ApprovalAction.APPROVE, Map.of(ApprovalStatus.PENDING, ApprovalStatus.APPROVED),
            ApprovalAction.REJECT, Map.of(ApprovalStatus.PENDING, ApprovalStatus.REJECTED),
            ApprovalAction.CANCEL, Map.of(
                    ApprovalStatus.DRAFT, ApprovalStatus.CANCELLED,
                    ApprovalStatus.PENDING, ApprovalStatus.CANCELLED));

    /**
     * 按动作与当前状态求新状态；非法流转抛 {@link BusinessException}。
     */
    public static ApprovalStatus next(ApprovalAction action, ApprovalStatus current) {
        ApprovalStatus next = TRANSITIONS.getOrDefault(action, Map.of()).get(current);
        if (next == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "非法状态流转: 当前[" + current.getDesc() + "] 执行[" + action.getDesc() + "]");
        }
        return next;
    }
}
