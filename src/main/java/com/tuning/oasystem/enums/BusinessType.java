package com.tuning.oasystem.enums;

import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.exception.BusinessException;

import java.util.Arrays;

/**
 * 审批业务类型
 */
public enum BusinessType {

    /** 请假 */
    LEAVE(1, "请假"),
    /** 报销 */
    REIMBURSEMENT(2, "报销");

    private final Integer value;

    private final String desc;

    BusinessType(Integer value, String desc) {
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
    public static BusinessType of(Integer value) {
        return Arrays.stream(values())
                .filter(b -> b.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "未知业务类型: " + value));
    }
}
