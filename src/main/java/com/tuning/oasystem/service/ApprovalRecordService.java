package com.tuning.oasystem.service;

import com.tuning.oasystem.enums.ApprovalAction;
import com.tuning.oasystem.enums.BusinessType;
import com.tuning.oasystem.vo.ApprovalRecordVO;

import java.util.List;

/**
 * 审批记录服务
 */
public interface ApprovalRecordService {

    /** 记录一条审批流水（提交/通过/拒绝/撤回） */
    ApprovalRecordVO record(BusinessType businessType, Long businessId, Long actorId,
                            ApprovalAction action, String comment);

    /** 按业务单查询审批记录（按时间升序） */
    List<ApprovalRecordVO> listByBusiness(BusinessType businessType, Long businessId);
}
