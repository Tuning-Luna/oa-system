package com.tuning.oasystem.storage;

import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘存储实现。
 * <p>
 * 根目录由 {@code upload.dir} 配置（默认 ./uploads）；对相对路径做归一化并校验前缀，
 * 防止路径穿越。后续如需 MinIO，新增实现类替换即可，业务层无需改动。
 */
@Slf4j
@Service
public class LocalStorageServiceImpl implements StorageService {

    private final Path root;

    public LocalStorageServiceImpl(@Value("${upload.dir:./uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("创建上传目录失败: " + root, e);
        }
    }

    @Override
    public String store(MultipartFile file, String storeName) {
        Path target = resolve(storeName);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件保存失败: " + storeName);
        }
        return storeName;
    }

    @Override
    public Resource loadAsResource(String storePath) {
        Path target = resolve(storePath);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在或已删除");
        }
        return new FileSystemResource(target);
    }

    @Override
    public void delete(String storePath) {
        try {
            Files.deleteIfExists(resolve(storePath));
        } catch (IOException e) {
            log.warn("物理文件删除失败: {}", storePath);
        }
    }

    /** 归一化并校验目标路径在根目录内（防路径穿越） */
    private Path resolve(String name) {
        Path target = root.resolve(name).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "非法文件路径");
        }
        return target;
    }
}
