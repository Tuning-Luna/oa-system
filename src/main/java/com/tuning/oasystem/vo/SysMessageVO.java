package com.tuning.oasystem.vo;

import com.tuning.oasystem.entity.SysMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知返回
 */
@Data
@Schema(description = "系统通知")
public class SysMessageVO {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "接收人ID")
    private Long receiverId;

    @Schema(description = "通知类型：APPROVAL 审批通知")
    private String type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "0未读 1已读")
    private Integer readFlag;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public static SysMessageVO from(SysMessage entity) {
        SysMessageVO vo = new SysMessageVO();
        vo.setId(entity.getId());
        vo.setReceiverId(entity.getReceiverId());
        vo.setType(entity.getType());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setReadFlag(entity.getReadFlag());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
