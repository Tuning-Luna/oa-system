package com.tuning.oasystem.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件存储抽象：本地磁盘 / MinIO 等实现可替换。
 * <p>
 * store 返回相对路径（upload-dir 下），load / delete 均以该相对路径操作。
 */
public interface StorageService {

    /** 保存文件，返回相对存储路径（如 {@code 20260802/uuid.png}） */
    String store(MultipartFile file) throws IOException;

    /** 读取文件资源（不存在抛业务异常） */
    Resource load(String relativePath);

    /** 删除物理文件 */
    void delete(String relativePath);
}
