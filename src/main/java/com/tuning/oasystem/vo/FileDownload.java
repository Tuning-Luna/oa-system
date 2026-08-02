package com.tuning.oasystem.vo;

import lombok.Data;
import org.springframework.core.io.Resource;

/**
 * 文件下载结果：资源流 + 元数据（供 Controller 组装响应头）
 */
@Data
public class FileDownload {

    /** 文件资源流 */
    private Resource resource;

    /** 原始文件名（Content-Disposition 用） */
    private String originalName;

    /** 大小（字节） */
    private long size;

    /** MIME 类型 */
    private String contentType;
}
