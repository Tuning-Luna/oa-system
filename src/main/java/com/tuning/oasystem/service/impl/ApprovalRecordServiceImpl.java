package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.entity.ApprovalRecord;
import com.tuning.oasystem.enums.ApprovalAction;
import com.tuning.oasystem.enums.BusinessType;
import com.tuning.oasystem.mapper.ApprovalRecordMapper;
import com.tuning.oasystem.service.ApprovalRecordService;
import com.tuning.oasystem.utils.UserNameResolver;
import com.tuning.oasystem.vo.ApprovalRecordVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 审批记录服务实现
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class ApprovalRecordServiceImpl implements ApprovalRecordService {

    private final ApprovalRecordMapper recordMapper;
    private final UserNameResolver userNameResolver;

    public ApprovalRecordServiceImpl(ApprovalRecordMapper recordMapper, UserNameResolver userNameResolver) {
        this.recordMapper = recordMapper;
        this.userNameResolver = userNameResolver;
    }

    @Override
    public ApprovalRecordVO record(BusinessType businessType, Long businessId, Long actorId,
                                   ApprovalAction action, String comment) {
        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType(businessType.getValue());
        record.setBusinessId(businessId);
        record.setApproverId(actorId);
        record.setAction(action.getValue());
        record.setComment(comment);
        recordMapper.insert(record);
        return ApprovalRecordVO.from(record, userNameResolver.nameOf(actorId));
    }

    @Override
    public List<ApprovalRecordVO> listByBusiness(BusinessType businessType, Long businessId) {
        List<ApprovalRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getBusinessType, businessType.getValue())
                        .eq(ApprovalRecord::getBusinessId, businessId)
                        .orderByAsc(ApprovalRecord::getId));
        Map<Long, String> names = userNameResolver.namesOf(records.stream().map(ApprovalRecord::getApproverId).toList());
        return records.stream()
                .map(r -> ApprovalRecordVO.from(r, names.get(r.getApproverId())))
                .toList();
    }
}
