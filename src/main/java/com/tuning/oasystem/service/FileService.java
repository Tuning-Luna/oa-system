package com.tuning.oasystem.service;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.dto.FileQuery;
import com.tuning.oasystem.vo.FileDownload;
import com.tuning.oasystem.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理服务
 */
public interface FileService {

    /** 上传文件（大小 + 类型白名单校验），返回文件信息 */
    FileVO upload(Long uploaderId, MultipartFile file);

    /** 下载文件（全员可下载） */
    FileDownload download(Long id);

    /** 分页查询（文件名/类型筛选，全员共享） */
    PageResult<FileVO> page(FileQuery query);

    /** 删除文件（仅上传者可删；逻辑 + 物理删除） */
    void delete(Long operatorId, Long id);
}
