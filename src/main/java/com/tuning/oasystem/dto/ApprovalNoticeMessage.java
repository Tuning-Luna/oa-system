package com.tuning.oasystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审批完成通知消息（经 RabbitMQ 传输）
 * <p>
 * 由 {@code ApprovalNoticeProducer} 在审批通过/拒绝时发送，{@code ApprovalNoticeConsumer}
 * 消费后为申请人写入系统通知。
 */
@Data
@NoArgsConstructor
public class ApprovalNoticeMessage {

    /** 业务类型：1请假 2报销 */
    private Integer businessType;

    /** 业务单号 */
    private Long businessId;

    /** 申请人ID（通知接收人） */
    private Long applicantId;

    /** 审批人ID */
    private Long approverId;

    /** 审批结果：2通过 3拒绝（ApprovalStatus 值） */
    private Integer result;

    /** 消息产生时间 */
    private LocalDateTime createTime;
}
