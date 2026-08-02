package com.tuning.oasystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知实体（sys_message）
 */
@Data
@TableName("sys_message")
public class SysMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人ID */
    private Long receiverId;

    /** 通知类型：APPROVAL 审批通知 */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 0未读 1已读 */
    private Integer readFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
