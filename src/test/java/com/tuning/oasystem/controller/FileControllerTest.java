package com.tuning.oasystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import com.tuning.oasystem.entity.FileInfo;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.mapper.FileInfoMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件接口集成测试（需 MySQL + Redis 运行）：上传校验、下载内容一致、共享列表、删除与权限。
 */
@SpringBootTest
@AutoConfigureMockMvc
// Eclipse 对 MyBatis-Plus LambdaQueryWrapper 方法引用做 null 分析时的误报，统一抑制
@SuppressWarnings("null")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private FileInfoMapper fileMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final List<String> createdUsernames = new ArrayList<>();
    private final List<Long> createdFileIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : createdFileIds) {
            FileInfo info = fileMapper.selectById(id);
            if (info != null) {
                try {
                    Files.deleteIfExists(Paths.get(uploadDir, info.getPath()));
                } catch (Exception ignore) {
                }
                fileMapper.deleteById(id);
            }
        }
        createdFileIds.clear();
        for (String username : createdUsernames) {
            sysUserMapper.delete(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        }
        createdUsernames.clear();
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private Long registerAndGetId(String username, String password) throws Exception {
        createdUsernames.add(username);
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"nickname\":\"测试\"}";
        MvcResult result = mockMvc
                .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private Long uploadFile(String token, String name, String contentType, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", name, contentType, content);
        MvcResult result = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        Long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
        createdFileIds.add(id);
        return id;
    }

    @Test
    void uploadDownloadSharedAndDelete() throws Exception {
        String usernameA = uniqueUsername("file_a");
        String usernameB = uniqueUsername("file_b");
        registerAndGetId(usernameA, "Passw0rd123");
        registerAndGetId(usernameB, "Passw0rd123");
        String tokenA = loginAndGetToken(usernameA, "Passw0rd123");
        String tokenB = loginAndGetToken(usernameB, "Passw0rd123");

        byte[] content = "hello file".getBytes(StandardCharsets.UTF_8);
        Long fileId = uploadFile(tokenA, "hello.txt", "text/plain", content);

        // B（非上传者）可下载（登录用户共享）
        MvcResult download = mockMvc.perform(get("/api/files/" + fileId + "/download")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        assertArrayEquals(content, download.getResponse().getContentAsByteArray());

        // B 的分页列表可见 A 的文件
        mockMvc.perform(get("/api/files").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        // B 不能删除 A 的文件（非上传者非管理员）
        mockMvc.perform(delete("/api/files/" + fileId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        // A 删除成功，删除后不可下载
        mockMvc.perform(delete("/api/files/" + fileId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/files/" + fileId + "/download").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadUnsupportedTypeShouldReturn400() throws Exception {
        String username = uniqueUsername("file_vd");
        registerAndGetId(username, "Passw0rd123");
        String token = loginAndGetToken(username, "Passw0rd123");

        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[10]);
        mockMvc.perform(multipart("/api/files/upload").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void unauthenticatedUploadShouldReturn401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[10]);
        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
