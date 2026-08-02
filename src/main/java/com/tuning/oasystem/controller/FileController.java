package com.tuning.oasystem.controller;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.Result;
import com.tuning.oasystem.dto.FileQuery;
import com.tuning.oasystem.security.LoginUser;
import com.tuning.oasystem.service.FileDownload;
import com.tuning.oasystem.service.FileService;
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
import org.springframework.web.bind.annotation.RequestPart;
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

    @Operation(summary = "上传文件", description = "multipart 表单字段 file；大小 ≤10MB，扩展名白名单（默认 office/图片/PDF）")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileVO> upload(@AuthenticationPrincipal LoginUser loginUser,
                                 @RequestPart("file") MultipartFile file) {
        return Result.success(fileService.upload(loginUser.getUserId(), file));
    }

    @Operation(summary = "下载文件", description = "登录用户可下载全部文件，按原始文件名下载")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileDownload download = fileService.download(id);
        String encoded = URLEncoder.encode(download.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(download.getContentType() != null
                        ? MediaType.parseMediaType(download.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(download.getResource());
    }

    @Operation(summary = "文件分页列表", description = "按原始文件名/类型模糊筛选；登录用户共享可见")
    @GetMapping
    public Result<PageResult<FileVO>> page(FileQuery query) {
        return Result.success(fileService.page(query));
    }

    @Operation(summary = "删除文件", description = "逻辑删除 + 物理文件清理；仅上传者或管理员")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        fileService.delete(loginUser.getUserId(), id);
        return Result.success();
    }
}
