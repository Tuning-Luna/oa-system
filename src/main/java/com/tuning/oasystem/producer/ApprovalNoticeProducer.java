package com.tuning.oasystem.producer;

import com.tuning.oasystem.config.RabbitConfig;
import com.tuning.oasystem.dto.ApprovalNoticeMessage;
import com.tuning.oasystem.enums.BusinessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审批通知生产者：审批通过/拒绝时发送消息。
 * <p>
 * 发送失败仅记录日志，不阻断审批主流程（通知为异步旁路）。
 */
@Slf4j
@Service
public class ApprovalNoticeProducer {

    private final RabbitTemplate rabbitTemplate;

    public ApprovalNoticeProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送审批完成通知。
     *
     * @param businessType 业务类型（请假/报销）
     * @param businessId   业务单号
     * @param applicantId  申请人ID（通知接收人）
     * @param approverId   审批人ID
     * @param result       审批结果：ApprovalStatus.APPROVED/REJECTED 值
     */
    public void sendApprovalNotice(BusinessType businessType, Long businessId,
            Long applicantId, Long approverId, Integer result) {
        ApprovalNoticeMessage message = new ApprovalNoticeMessage();
        message.setBusinessType(businessType.getValue());
        message.setBusinessId(businessId);
        message.setApplicantId(applicantId);
        message.setApproverId(approverId);
        message.setResult(result);
        message.setCreateTime(LocalDateTime.now());
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, message);
        } catch (Exception e) {
            log.warn("审批通知发送失败（不影响审批结果）: {}", e.getMessage());
        }
    }
}
