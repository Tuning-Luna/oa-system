package com.tuning.oasystem.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象：屏蔽底层存储实现（本地磁盘 / 未来 MinIO 等）。
 * <p>
 * 所有操作基于存储相对路径（如 {@code 2026/08/uuid.png}）。
 */
public interface StorageService {

    /** 保存文件到存储，返回存储相对路径 */
    String store(MultipartFile file, String storeName);

    /** 按相对路径读取文件资源；不存在抛业务异常 */
    Resource loadAsResource(String storePath);

    /** 按相对路径删除物理文件（失败仅记录日志） */
    void delete(String storePath);
}
