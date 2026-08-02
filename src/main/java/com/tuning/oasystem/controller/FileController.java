package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.dto.FileQuery;
import com.tuning.oasystem.security.LoginUser;
import com.tuning.oasystem.service.FileService;
import com.tuning.oasystem.vo.FileDownload;
import com.tuning.oasystem.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件管理接口
 */
@Tag(name = "文件管理", description = "文件上传 / 下载 / 分页列表 / 删除")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "上传文件", description = "multipart/form-data，字段名 file；限 20MB + 类型白名单")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileVO> upload(@AuthenticationPrincipal LoginUser loginUser,
                                 @RequestParam("file") MultipartFile file) {
        return Result.success(fileService.upload(loginUser.getUserId(), file));
    }

    @Operation(summary = "下载文件", description = "全员可下载，返回文件流")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileDownload download = fileService.download(id);
        String contentType = download.getContentType() != null
                ? download.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String encodedName = URLEncoder.encode(download.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(download.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .body(download.getResource());
    }

    @Operation(summary = "文件分页列表", description = "按文件名/类型筛选，全员共享")
    @GetMapping
    public Result<PageResult<FileVO>> page(FileQuery query) {
        return Result.success(fileService.page(query));
    }

    @Operation(summary = "删除文件", description = "仅上传者可删除；逻辑删除 + 物理文件删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        fileService.delete(loginUser.getUserId(), id);
        return Result.success();
    }
}
