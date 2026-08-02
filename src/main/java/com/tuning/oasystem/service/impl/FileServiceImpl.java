package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.FileQuery;
import com.tuning.oasystem.entity.FileInfo;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.FileInfoMapper;
import com.tuning.oasystem.service.FileService;
import com.tuning.oasystem.storage.StorageService;
import com.tuning.oasystem.utils.UserNameResolver;
import com.tuning.oasystem.vo.FileDownload;
import com.tuning.oasystem.vo.FileVO;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件管理服务实现
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class FileServiceImpl implements FileService {

    private static final long MAX_SIZE = 20L * 1024 * 1024; // 20MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "pdf",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "txt");
    private static final long DEFAULT_PAGE_SIZE = 10;
    private static final long MAX_PAGE_SIZE = 100;

    private final FileInfoMapper fileMapper;
    private final StorageService storage;
    private final UserNameResolver userNameResolver;

    public FileServiceImpl(FileInfoMapper fileMapper,
            StorageService storage,
            UserNameResolver userNameResolver) {
        this.fileMapper = fileMapper;
        this.storage = storage;
        this.userNameResolver = userNameResolver;
    }

    @Override
    @Transactional
    public FileVO upload(Long uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件大小超过 20MB 限制");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的文件类型，仅允许: " + ALLOWED_EXTENSIONS);
        }

        String storeName = buildStoreName(extension);
        storage.store(file, storeName);

        FileInfo entity = new FileInfo();
        entity.setOriginalName(file.getOriginalFilename());
        entity.setStoreName(storeName);
        entity.setPath(storeName);
        entity.setSize(file.getSize());
        entity.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        entity.setUploaderId(uploaderId);
        fileMapper.insert(entity);
        entity.setUrl("/api/files/" + entity.getId() + "/download");
        fileMapper.updateById(entity);
        return FileVO.from(entity, userNameResolver.nameOf(uploaderId));
    }

    @Override
    public FileDownload download(Long id) {
        FileInfo file = requireFile(id);
        Resource resource = storage.loadAsResource(file.getPath());
        FileDownload download = new FileDownload();
        download.setResource(resource);
        download.setOriginalName(file.getOriginalName());
        download.setSize(file.getSize() == null ? 0 : file.getSize());
        download.setContentType(file.getContentType());
        return download;
    }

    @Override
    public PageResult<FileVO> page(FileQuery query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(query.getPageSize(), MAX_PAGE_SIZE);

        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .like(StringUtils.hasText(query.getFileName()), FileInfo::getOriginalName, query.getFileName())
                .like(StringUtils.hasText(query.getContentType()), FileInfo::getContentType, query.getContentType())
                .orderByDesc(FileInfo::getId);

        Page<FileInfo> page = fileMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> names = userNameResolver.namesOf(
                page.getRecords().stream().map(FileInfo::getUploaderId).toList());
        List<FileVO> records = page.getRecords().stream()
                .map(f -> FileVO.from(f, names.get(f.getUploaderId())))
                .toList();
        return PageResult.of(page.getTotal(), records, page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public void delete(Long operatorId, Long id) {
        FileInfo file = requireFile(id);
        if (!file.getUploaderId().equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅上传者可删除该文件");
        }
        fileMapper.deleteById(id);
        storage.delete(file.getPath());
    }

    private FileInfo requireFile(Long id) {
        FileInfo file = fileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在: id=" + id);
        }
        return file;
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return null;
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return null;
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /** 存储相对路径：yyyy/MM/uuid.ext（按月分目录，避免单目录文件过多） */
    private String buildStoreName(String extension) {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }
}
