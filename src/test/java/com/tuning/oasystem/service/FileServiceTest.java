package com.tuning.oasystem.service;

import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.FileQuery;
import com.tuning.oasystem.entity.FileInfo;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.FileInfoMapper;
import com.tuning.oasystem.storage.StorageService;
import com.tuning.oasystem.vo.FileDownload;
import com.tuning.oasystem.vo.FileVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件服务测试：上传（大小/类型校验）、下载内容一致、分页筛选、逻辑+物理删除、删除归属校验。
 */
@SpringBootTest
class FileServiceTest {

    private static final Long UPLOADER_A = 100001L;
    private static final Long UPLOADER_B = 100002L;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileInfoMapper fileMapper;

    @Autowired
    private StorageService storage;

    private final List<Long> createdIds = new ArrayList<>();
    private final List<String> createdPaths = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String path : createdPaths) {
            storage.delete(path);
        }
        createdPaths.clear();
        for (Long id : createdIds) {
            fileMapper.deleteById(id);
        }
        createdIds.clear();
    }

    private FileVO upload(Long uploader, String fileName, byte[] bytes) {
        MockMultipartFile mf = new MockMultipartFile("file", fileName, "application/octet-stream", bytes);
        FileVO vo = fileService.upload(uploader, mf);
        createdIds.add(vo.getId());
        FileInfo info = fileMapper.selectById(vo.getId());
        createdPaths.add(info.getPath());
        return vo;
    }

    @Test
    void uploadShouldPersistAndStorePhysical() throws Exception {
        byte[] content = "hello file content".getBytes();
        FileVO vo = upload(UPLOADER_A, "test.png", content);

        assertNotNull(vo.getId());
        assertEquals("test.png", vo.getFileName());
        assertEquals(content.length, vo.getSize());
        assertTrue(vo.getUrl().endsWith("/download"));
        assertEquals(UPLOADER_A, vo.getUploaderId());

        // 物理文件已落盘，内容一致
        Resource resource = storage.loadAsResource(createdPaths.get(0));
        assertEquals(content.length, resource.contentLength());
    }

    @Test
    void uploadOversizeShouldThrow() {
        byte[] big = new byte[20 * 1024 * 1024 + 1];
        MockMultipartFile mf = new MockMultipartFile("file", "big.bin", "application/octet-stream", big);
        BusinessException ex = assertThrows(BusinessException.class, () -> fileService.upload(UPLOADER_A, mf));
        assertTrue(ex.getMessage().contains("20MB"));
    }

    @Test
    void uploadUnsupportedTypeShouldThrow() {
        MockMultipartFile mf = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "x".getBytes());
        BusinessException ex = assertThrows(BusinessException.class, () -> fileService.upload(UPLOADER_A, mf));
        assertTrue(ex.getMessage().contains("不支持的文件类型"));
    }

    @Test
    void downloadShouldReturnMatchingContent() throws Exception {
        byte[] content = "pdf-data-1234567890".getBytes();
        FileVO vo = upload(UPLOADER_A, "report.pdf", content);

        FileDownload download = fileService.download(vo.getId());
        assertEquals("report.pdf", download.getOriginalName());
        assertEquals(content.length, download.getSize());
        try (InputStream is = download.getResource().getInputStream()) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    @Test
    void pageShouldFilterByFileName() {
        upload(UPLOADER_A, "alpha.png", "a".getBytes());
        upload(UPLOADER_A, "beta.txt", "b".getBytes());

        FileQuery query = new FileQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setFileName("alpha");
        PageResult<FileVO> result = fileService.page(query);

        assertEquals(1, result.getTotal());
        assertEquals("alpha.png", result.getRecords().get(0).getFileName());
    }

    @Test
    void deleteShouldRemoveRecordAndPhysical() {
        byte[] content = "delete-me".getBytes();
        FileVO vo = upload(UPLOADER_A, "tmp.png", content);

        fileService.delete(UPLOADER_A, vo.getId());

        assertNull(fileMapper.selectById(vo.getId()), "逻辑删除后查不到记录");
        assertThrows(BusinessException.class, () -> storage.loadAsResource(createdPaths.get(0)),
                "物理文件应被删除");
    }

    @Test
    void deleteByNonUploaderShouldThrow() {
        FileVO vo = upload(UPLOADER_A, "owned.png", "x".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> fileService.delete(UPLOADER_B, vo.getId()));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        // 未被删除，仍可下载
        assertNotNull(fileService.download(vo.getId()).getResource());
    }
}
