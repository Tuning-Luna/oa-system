package com.tuning.oasystem.vo;

import com.tuning.oasystem.entity.FileInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息返回（不回传磁盘路径）
 */
@Data
@Schema(description = "文件信息")
public class FileVO {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long size;

    @Schema(description = "MIME 类型")
    private String contentType;

    @Schema(description = "访问地址")
    private String url;

    @Schema(description = "上传人ID")
    private Long uploaderId;

    @Schema(description = "上传人姓名")
    private String uploaderName;

    @Schema(description = "上传时间")
    private LocalDateTime createTime;

    public static FileVO from(FileInfo file, String uploaderName) {
        FileVO vo = new FileVO();
        vo.setId(file.getId());
        vo.setFileName(file.getOriginalName());
        vo.setSize(file.getSize());
        vo.setContentType(file.getContentType());
        vo.setUrl(file.getUrl());
        vo.setUploaderId(file.getUploaderId());
        vo.setUploaderName(uploaderName);
        vo.setCreateTime(file.getCreateTime());
        return vo;
    }
}
