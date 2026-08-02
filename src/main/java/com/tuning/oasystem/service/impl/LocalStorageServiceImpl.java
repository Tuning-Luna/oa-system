package com.tuning.oasystem.service.impl;

import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.service.StorageService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘存储实现。
 * <p>
 * 目录结构：{@code upload-dir/yyyyMMdd/uuid.ext}；load 时做路径穿越防护。
 */
@Slf4j
@Service
public class LocalStorageServiceImpl implements StorageService {

    private final Path uploadPath;

    public LocalStorageServiceImpl(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dir = uploadPath.resolve(dateDir);
        Files.createDirectories(dir);
        String storeName = UUID.randomUUID().toString().replace("-", "") + extension(file.getOriginalFilename());
        file.transferTo(dir.resolve(storeName));
        return dateDir + "/" + storeName;
    }

    @Override
    public Resource load(String relativePath) {
        Path file = resolveSafe(relativePath);
        if (!Files.exists(file)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        return new FileSystemResource(file);
    }

    @Override
    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolveSafe(relativePath));
        } catch (IOException e) {
            log.warn("物理文件删除失败: {} - {}", relativePath, e.getMessage());
        }
    }

    /** 路径穿越防护：仅允许访问 upload-dir 内的文件 */
    private Path resolveSafe(String relativePath) {
        Path file = uploadPath.resolve(relativePath).normalize();
        if (!file.startsWith(uploadPath)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "非法文件路径");
        }
        return file;
    }

    private String extension(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        return dot < 0 ? "" : originalName.substring(dot);
    }
}
