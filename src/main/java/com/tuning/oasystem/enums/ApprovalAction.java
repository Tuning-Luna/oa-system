package com.tuning.oasystem.enums;

/**
 * 审批动作（对应审批记录 action）
 */
public enum ApprovalAction {

    /** 提交 */
    SUBMIT(1, "提交"),
    /** 通过 */
    APPROVE(2, "通过"),
    /** 拒绝 */
    REJECT(3, "拒绝"),
    /** 撤回 */
    CANCEL(4, "撤回");

    private final Integer value;

    private final String desc;

    ApprovalAction(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public Integer getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }
}
