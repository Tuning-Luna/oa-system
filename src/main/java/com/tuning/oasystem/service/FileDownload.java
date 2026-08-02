package com.tuning.oasystem.service;

import org.springframework.core.io.Resource;

/**
 * 文件下载载体：原始文件名 + 内容类型 + 文件资源（供 Controller 组装下载响应）。
 */
public class FileDownload {

    private final String originalName;

    private final String contentType;

    private final Resource resource;

    public FileDownload(String originalName, String contentType, Resource resource) {
        this.originalName = originalName;
        this.contentType = contentType;
        this.resource = resource;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public Resource getResource() {
        return resource;
    }
}
