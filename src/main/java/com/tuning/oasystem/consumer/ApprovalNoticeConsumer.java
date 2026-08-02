package com.tuning.oasystem.consumer;

import com.tuning.oasystem.config.RabbitConfig;
import com.tuning.oasystem.dto.ApprovalNoticeMessage;
import com.tuning.oasystem.entity.SysMessage;
import com.tuning.oasystem.enums.ApprovalStatus;
import com.tuning.oasystem.enums.BusinessType;
import com.tuning.oasystem.mapper.SysMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 审批通知消费者：消费消息 → 为申请人写入系统通知（sys_message）。
 */
@Slf4j
@Component
public class ApprovalNoticeConsumer {

    /** 通知类型：审批通知 */
    public static final String TYPE_APPROVAL = "APPROVAL";

    private final SysMessageMapper messageMapper;

    public ApprovalNoticeConsumer(SysMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onMessage(ApprovalNoticeMessage message) {
        if (message == null || message.getApplicantId() == null
                || message.getBusinessType() == null || message.getResult() == null) {
            log.warn("审批通知消息不完整，忽略");
            return;
        }
        String typeDesc = BusinessType.of(message.getBusinessType()).getDesc();
        String resultDesc = ApprovalStatus.of(message.getResult()).getDesc();

        SysMessage notice = new SysMessage();
        notice.setReceiverId(message.getApplicantId());
        notice.setType(TYPE_APPROVAL);
        notice.setTitle(typeDesc + "申请审批结果");
        notice.setContent("您的" + typeDesc + "申请（单号 " + message.getBusinessId() + "）"
                + resultDesc + "，审批人ID：" + message.getApproverId());
        notice.setReadFlag(0);
        messageMapper.insert(notice);
        log.info("已写入审批通知 receiverId={} businessId={}", message.getApplicantId(), message.getBusinessId());
    }
}
