package com.tuning.oasystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.MessageQuery;
import com.tuning.oasystem.entity.SysMessage;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.SysMessageMapper;
import com.tuning.oasystem.vo.SysMessageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 系统通知服务测试：分页筛选、标记已读（归属校验）、全部已读、未读计数。
 */
@SpringBootTest
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class MessageServiceTest {

    private static final Long RECEIVER = 999001L;
    private static final Long OTHER = 999002L;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SysMessageMapper messageMapper;

    private final List<Long> createdMessageIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : createdMessageIds) {
            messageMapper.deleteById(id);
        }
        createdMessageIds.clear();
    }

    private Long insertMessage(Long receiver, Integer readFlag) {
        SysMessage m = new SysMessage();
        m.setReceiverId(receiver);
        m.setType("APPROVAL");
        m.setTitle("测试通知");
        m.setContent("测试内容");
        m.setReadFlag(readFlag);
        messageMapper.insert(m);
        createdMessageIds.add(m.getId());
        return m.getId();
    }

    private MessageQuery query(Long pageNum, Long pageSize, Integer readFlag) {
        MessageQuery q = new MessageQuery();
        q.setPageNum(pageNum);
        q.setPageSize(pageSize);
        q.setReadFlag(readFlag);
        return q;
    }

    @Test
    void pageShouldFilterByReadFlag() {
        insertMessage(RECEIVER, 0);
        insertMessage(RECEIVER, 0);
        insertMessage(RECEIVER, 1);

        PageResult<SysMessageVO> all = messageService.page(RECEIVER, query(1L, 10L, null));
        assertEquals(3, all.getTotal());

        PageResult<SysMessageVO> unread = messageService.page(RECEIVER, query(1L, 10L, 0));
        assertEquals(2, unread.getTotal());

        PageResult<SysMessageVO> read = messageService.page(RECEIVER, query(1L, 10L, 1));
        assertEquals(1, read.getTotal());
    }

    @Test
    void markReadShouldOwnAndUpdate() {
        Long id = insertMessage(RECEIVER, 0);

        messageService.markRead(RECEIVER, id);
        assertEquals(1, messageMapper.selectById(id).getReadFlag());

        // 他人标记 → 403
        Long otherId = insertMessage(RECEIVER, 0);
        BusinessException ex = assertThrows(BusinessException.class, () -> messageService.markRead(OTHER, otherId));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void markAllReadAndUnreadCount() {
        insertMessage(RECEIVER, 0);
        insertMessage(RECEIVER, 0);
        insertMessage(RECEIVER, 1);

        assertEquals(2, messageService.unreadCount(RECEIVER));

        messageService.markAllRead(RECEIVER);
        assertEquals(0, messageService.unreadCount(RECEIVER));
        assertEquals(0, messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, RECEIVER)
                .eq(SysMessage::getReadFlag, 0)));
    }
}
