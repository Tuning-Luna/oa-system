package com.tuning.oasystem.enums;

import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.common.ResultCode;

import java.util.Arrays;

/**
 * 审批单状态（状态机值）
 * <p>
 * DRAFT → PENDING → APPROVED / REJECTED；DRAFT/PENDING 可撤回 → CANCELLED
 */
public enum ApprovalStatus {

    /** 草稿 */
    DRAFT(0, "草稿"),
    /** 待审批 */
    PENDING(1, "待审批"),
    /** 已通过 */
    APPROVED(2, "已通过"),
    /** 已拒绝 */
    REJECTED(3, "已拒绝"),
    /** 已撤回 */
    CANCELLED(4, "已撤回");

    private final Integer value;

    private final String desc;

    ApprovalStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public Integer getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    /** 由存储值反查枚举，未知值抛业务异常 */
    public static ApprovalStatus of(Integer value) {
        return Arrays.stream(values())
                .filter(s -> s.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.INTERNAL_ERROR, "未知审批状态: " + value));
    }
}
