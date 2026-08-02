package com.tuning.oasystem.enums;

import com.tuning.oasystem.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 状态机单元测试：遍历全部合法流转，并断言非法流转必抛异常。
 */
class ApprovalStateMachineTest {

    @Test
    void validTransitionsShouldMoveToNextState() {
        assertEquals(ApprovalStatus.PENDING, ApprovalStateMachine.next(ApprovalAction.SUBMIT, ApprovalStatus.DRAFT));
        assertEquals(ApprovalStatus.APPROVED, ApprovalStateMachine.next(ApprovalAction.APPROVE, ApprovalStatus.PENDING));
        assertEquals(ApprovalStatus.REJECTED, ApprovalStateMachine.next(ApprovalAction.REJECT, ApprovalStatus.PENDING));
        assertEquals(ApprovalStatus.CANCELLED, ApprovalStateMachine.next(ApprovalAction.CANCEL, ApprovalStatus.DRAFT));
        assertEquals(ApprovalStatus.CANCELLED, ApprovalStateMachine.next(ApprovalAction.CANCEL, ApprovalStatus.PENDING));
    }

    @Test
    void invalidTransitionsShouldThrow() {
        // 未提交不可审批 / 拒绝
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.APPROVE, ApprovalStatus.DRAFT));
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.REJECT, ApprovalStatus.DRAFT));

        // 已提交不可重复提交
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.SUBMIT, ApprovalStatus.PENDING));

        // 已通过/已拒绝/已撤回不可再审批或撤回
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.APPROVE, ApprovalStatus.APPROVED));
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.REJECT, ApprovalStatus.APPROVED));
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.CANCEL, ApprovalStatus.APPROVED));
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.APPROVE, ApprovalStatus.REJECTED));
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.CANCEL, ApprovalStatus.REJECTED));
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.APPROVE, ApprovalStatus.CANCELLED));
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.CANCEL, ApprovalStatus.CANCELLED));
        assertThrows(BusinessException.class, () -> ApprovalStateMachine.next(ApprovalAction.SUBMIT, ApprovalStatus.CANCELLED));
    }
}
