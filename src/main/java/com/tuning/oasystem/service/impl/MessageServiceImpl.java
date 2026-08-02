package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.MessageQuery;
import com.tuning.oasystem.entity.SysMessage;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.SysMessageMapper;
import com.tuning.oasystem.service.MessageService;
import com.tuning.oasystem.vo.SysMessageVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统通知服务实现
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class MessageServiceImpl implements MessageService {

    private static final long DEFAULT_PAGE_SIZE = 10;
    private static final long MAX_PAGE_SIZE = 100;

    private final SysMessageMapper messageMapper;

    public MessageServiceImpl(SysMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public PageResult<SysMessageVO> page(Long receiverId, MessageQuery query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(query.getPageSize(), MAX_PAGE_SIZE);

        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, receiverId)
                .eq(query.getReadFlag() != null, SysMessage::getReadFlag, query.getReadFlag())
                .orderByDesc(SysMessage::getId);

        Page<SysMessage> page = messageMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<SysMessageVO> records = page.getRecords().stream().map(SysMessageVO::from).toList();
        return PageResult.of(page.getTotal(), records, page.getCurrent(), page.getSize());
    }

    @Override
    public void markRead(Long receiverId, Long id) {
        SysMessage message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在: id=" + id);
        }
        if (!message.getReceiverId().equals(receiverId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该通知");
        }
        message.setReadFlag(1);
        messageMapper.updateById(message);
    }

    @Override
    public void markAllRead(Long receiverId) {
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, receiverId)
                .eq(SysMessage::getReadFlag, 0)
                .set(SysMessage::getReadFlag, 1));
    }

    @Override
    public long unreadCount(Long receiverId) {
        Long count = messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, receiverId)
                .eq(SysMessage::getReadFlag, 0));
        return count == null ? 0 : count;
    }
}
