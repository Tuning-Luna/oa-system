package com.tuning.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.FileQuery;
import com.tuning.oasystem.entity.FileInfo;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.FileInfoMapper;
import com.tuning.oasystem.service.FileDownload;
import com.tuning.oasystem.service.FileService;
import com.tuning.oasystem.service.PermissionService;
import com.tuning.oasystem.service.StorageService;
import com.tuning.oasystem.utils.UserNameResolver;
import com.tuning.oasystem.vo.FileVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 文件管理服务实现
 */
@Service
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
public class FileServiceImpl implements FileService {

    private static final long DEFAULT_PAGE_SIZE = 10;
    private static final long MAX_PAGE_SIZE = 100;

    private final FileInfoMapper fileMapper;
    private final StorageService storageService;
    private final UserNameResolver userNameResolver;
    private final PermissionService permissionService;

    @Value("${file.max-size-mb:10}")
    private long maxSizeMb;

    @Value("${file.allowed-extensions:}")
    private String allowedExtensions;

    public FileServiceImpl(FileInfoMapper fileMapper,
            StorageService storageService,
            UserNameResolver userNameResolver,
            PermissionService permissionService) {
        this.fileMapper = fileMapper;
        this.storageService = storageService;
        this.userNameResolver = userNameResolver;
        this.permissionService = permissionService;
    }

    @Override
    public FileVO upload(Long uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > maxSizeMb * 1024 * 1024) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件大小超过限制（最大 " + maxSizeMb + "MB）");
        }
        validateExtension(file.getOriginalFilename());

        String relativePath;
        try {
            relativePath = storageService.store(file);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件存储失败");
        }

        FileInfo info = new FileInfo();
        info.setOriginalName(file.getOriginalFilename());
        info.setStoreName(relativePath.substring(relativePath.lastIndexOf('/') + 1));
        info.setPath(relativePath);
        info.setSize(file.getSize());
        info.setContentType(file.getContentType());
        info.setUploaderId(uploaderId);
        fileMapper.insert(info);
        info.setUrl("/api/files/" + info.getId() + "/download");
        fileMapper.updateById(info);

        return FileVO.from(info, userNameResolver.nameOf(uploaderId));
    }

    @Override
    public FileDownload download(Long id) {
        FileInfo info = requireFile(id);
        return new FileDownload(info.getOriginalName(), info.getContentType(), storageService.load(info.getPath()));
    }

    @Override
    public PageResult<FileVO> page(FileQuery query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(query.getPageSize(), MAX_PAGE_SIZE);

        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .like(StringUtils.hasText(query.getName()), FileInfo::getOriginalName, query.getName())
                .like(StringUtils.hasText(query.getContentType()), FileInfo::getContentType, query.getContentType())
                .orderByDesc(FileInfo::getId);

        Page<FileInfo> page = fileMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> names = userNameResolver.namesOf(page.getRecords().stream().map(FileInfo::getUploaderId).toList());
        List<FileVO> records = page.getRecords().stream()
                .map(r -> FileVO.from(r, names.get(r.getUploaderId())))
                .toList();
        return PageResult.of(page.getTotal(), records, page.getCurrent(), page.getSize());
    }

    @Override
    public void delete(Long operatorId, Long id) {
        FileInfo info = requireFile(id);
        boolean isAdmin = permissionService.getRoleCodesByUserId(operatorId).contains("admin");
        if (!info.getUploaderId().equals(operatorId) && !isAdmin) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅上传者或管理员可删除该文件");
        }
        fileMapper.deleteById(id);
        storageService.delete(info.getPath());
    }

    private FileInfo requireFile(Long id) {
        FileInfo info = fileMapper.selectById(id);
        if (info == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在: id=" + id);
        }
        return info;
    }

    private void validateExtension(String originalName) {
        if (!StringUtils.hasText(allowedExtensions) || originalName == null) {
            return;
        }
        int dot = originalName.lastIndexOf('.');
        String ext = dot < 0 ? "" : originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的文件类型: " + ext);
        }
    }
}
