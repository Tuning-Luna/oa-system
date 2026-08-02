package com.tuning.oasystem.service;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.dto.FileQuery;
import com.tuning.oasystem.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理服务
 */
public interface FileService {

    /** 上传文件（校验大小/扩展名） */
    FileVO upload(Long uploaderId, MultipartFile file);

    /** 下载文件（登录用户共享） */
    FileDownload download(Long id);

    /** 文件分页列表（按名称/类型筛选，登录用户共享） */
    PageResult<FileVO> page(FileQuery query);

    /** 删除文件（逻辑删除 + 物理清理；仅上传者或管理员） */
    void delete(Long operatorId, Long id);
}
