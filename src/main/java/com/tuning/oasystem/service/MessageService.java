package com.tuning.oasystem.service;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.dto.MessageQuery;
import com.tuning.oasystem.vo.SysMessageVO;

/**
 * 系统通知服务
 */
public interface MessageService {

    /** 我的通知分页（按未读状态筛选可选） */
    PageResult<SysMessageVO> page(Long receiverId, MessageQuery query);

    /** 标记单条已读（校验归属） */
    void markRead(Long receiverId, Long id);

    /** 全部标记已读 */
    void markAllRead(Long receiverId);

    /** 未读数量 */
    long unreadCount(Long receiverId);
}
