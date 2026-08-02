package com.tuning.oasystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuning.oasystem.common.PageResult;
import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.dto.FileQuery;
import com.tuning.oasystem.entity.FileInfo;
import com.tuning.oasystem.entity.SysRole;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.mapper.FileInfoMapper;
import com.tuning.oasystem.mapper.SysRoleMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.vo.FileVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件服务测试（需 MySQL 运行）：上传落库+物理文件、大小/类型校验、下载、共享列表、删除权限。
 * 通过 properties 把单文件上限压到 1MB 以便低成本触发超限校验。
 */
@SpringBootTest(properties = "file.max-size-mb=1")
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class FileServiceTest {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileInfoMapper fileMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private UserService userService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdFileIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : createdFileIds) {
            FileInfo info = fileMapper.selectById(id);
            if (info != null) {
                fileServiceDeletePhysical(info);
                fileMapper.deleteById(id);
            }
        }
        createdFileIds.clear();
        for (Long uid : createdUserIds) {
            sysUserMapper.deleteById(uid);
        }
        createdUserIds.clear();
    }

    private void fileServiceDeletePhysical(FileInfo info) {
        try {
            Files.deleteIfExists(Paths.get(uploadDir, info.getPath()));
        } catch (Exception ignore) {
            // 物理文件清理失败不影响测试结果断言
        }
    }

    private Long createUser(String prefix) {
        SysUser u = new SysUser();
        u.setUsername(prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        u.setPassword("$2a$10$d4RHUelT9g8KZoidi6pTAO2m.VGr5Dc0Acc6u.d7ga1pSB3TZLEHO");
        u.setNickname("测试用户");
        u.setStatus(1);
        sysUserMapper.insert(u);
        createdUserIds.add(u.getId());
        return u.getId();
    }

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    @Test
    void uploadShouldPersistAndCreatePhysicalFile() {
        Long uploader = createUser("file_up");
        byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);

        FileVO vo = fileService.upload(uploader, file("report.txt", "text/plain", content));
        createdFileIds.add(vo.getId());

        assertNotNull(vo.getId());
        assertEquals("report.txt", vo.getOriginalName());
        assertEquals((long) content.length, vo.getSize());
        assertEquals("/api/files/" + vo.getId() + "/download", vo.getUrl());

        FileInfo info = fileMapper.selectById(vo.getId());
        assertTrue(Files.exists(Paths.get(uploadDir, info.getPath())), "物理文件应写入磁盘");
    }

    @Test
    void uploadOversizeShouldThrow() {
        Long uploader = createUser("file_big");
        byte[] big = new byte[2 * 1024 * 1024]; // 2MB > 1MB 上限
        BusinessException ex = assertThrows(BusinessException.class,
                () -> fileService.upload(uploader, file("big.bin", "application/octet-stream", big)));
        assertTrue(ex.getMessage().contains("大小超过限制"));
    }

    @Test
    void uploadUnsupportedTypeShouldThrow() {
        Long uploader = createUser("file_bad");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> fileService.upload(uploader, file("virus.exe", "application/octet-stream", new byte[10])));
        assertTrue(ex.getMessage().contains("不支持的文件类型"));
    }

    @Test
    void downloadShouldReturnOriginalContent() throws Exception {
        Long uploader = createUser("file_dl");
        byte[] content = "download me".getBytes(StandardCharsets.UTF_8);
        FileVO vo = fileService.upload(uploader, file("hello.txt", "text/plain", content));
        createdFileIds.add(vo.getId());

        FileDownload download = fileService.download(vo.getId());
        assertEquals("hello.txt", download.getOriginalName());
        try (InputStream in = download.getResource().getInputStream()) {
            assertArrayEquals(content, in.readAllBytes());
        }
    }

    @Test
    void pageShouldFilterByNameAndShareAll() {
        Long uploaderA = createUser("file_a");
        Long uploaderB = createUser("file_b");
        String prefix = "shared_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        FileVO voA = fileService.upload(uploaderA, file(prefix + "_a.txt", "text/plain", "a".getBytes()));
        FileVO voB = fileService.upload(uploaderB, file(prefix + "_b.txt", "text/plain", "b".getBytes()));
        createdFileIds.add(voA.getId());
        createdFileIds.add(voB.getId());

        FileQuery query = new FileQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setName(prefix);
        PageResult<FileVO> page = fileService.page(query);
        assertEquals(2, page.getTotal(), "登录用户共享：分页应返回全部上传者文件");
    }

    @Test
    void deleteByOtherNonAdminShouldForbidden() {
        Long uploader = createUser("file_own");
        Long other = createUser("file_oth");
        FileVO vo = fileService.upload(uploader, file("own.txt", "text/plain", "x".getBytes()));
        createdFileIds.add(vo.getId());

        BusinessException ex = assertThrows(BusinessException.class, () -> fileService.delete(other, vo.getId()));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void adminCanDeleteOthersFile() {
        Long uploader = createUser("file_own");
        // 构造管理员：创建用户并分配 admin 角色
        Long admin = createUser("file_admin");
        SysRole adminRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "admin"));
        assertNotNull(adminRole, "依赖 V3 初始化数据：admin 角色");
        userService.assignRoles(admin, List.of(adminRole.getId()));

        FileVO vo = fileService.upload(uploader, file("admin.txt", "text/plain", "x".getBytes()));
        createdFileIds.add(vo.getId());

        fileService.delete(admin, vo.getId());
        createdFileIds.remove(vo.getId());
        assertThrows(BusinessException.class, () -> fileService.download(vo.getId()), "删除后不可下载");
    }
}
