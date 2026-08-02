package com.tuning.oasystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件分页查询参数
 */
@Data
@Schema(description = "文件分页查询参数")
public class FileQuery {

    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNum;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize;

    @Schema(description = "原始文件名（模糊）")
    private String name;

    @Schema(description = "MIME 类型（模糊）")
    private String contentType;
}
