package com.tuning.oasystem.vo;

import com.tuning.oasystem.entity.FileInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息返回
 */
@Data
@Schema(description = "文件信息")
public class FileVO {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "文件大小（字节）")
    private Long size;

    @Schema(description = "MIME 类型")
    private String contentType;

    @Schema(description = "下载 URL")
    private String url;

    @Schema(description = "上传人ID")
    private Long uploaderId;

    @Schema(description = "上传人姓名")
    private String uploaderName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public static FileVO from(FileInfo info, String uploaderName) {
        FileVO vo = new FileVO();
        vo.setId(info.getId());
        vo.setOriginalName(info.getOriginalName());
        vo.setSize(info.getSize());
        vo.setContentType(info.getContentType());
        vo.setUrl(info.getUrl());
        vo.setUploaderId(info.getUploaderId());
        vo.setUploaderName(uploaderName);
        vo.setCreateTime(info.getCreateTime());
        return vo;
    }
}
