package com.tuning.oasystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import com.tuning.oasystem.entity.FileInfo;
import com.tuning.oasystem.entity.SysUser;
import com.tuning.oasystem.mapper.FileInfoMapper;
import com.tuning.oasystem.mapper.SysUserMapper;
import com.tuning.oasystem.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件管理接口集成测试（需 MySQL 运行）：上传 → 下载内容一致 → 分页 → 删除后下载 404；未认证 401；类型校验 400。
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

    @Autowired
    private StorageService storage;

    private final List<String> createdUsernames = new ArrayList<>();
    private final List<Long> createdFileIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : createdFileIds) {
            FileInfo info = fileMapper.selectById(id);
            if (info != null) {
                storage.delete(info.getPath());
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

    private String registerAndLogin() throws Exception {
        String username = uniqueUsername("file");
        createdUsernames.add(username);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Passw0rd123\",\"nickname\":\"f\"}"))
                .andExpect(status().isOk());
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Passw0rd123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    void uploadDownloadPageDeleteFlow() throws Exception {
        String token = registerAndLogin();
        byte[] content = "file-content-xyz-12345".getBytes();
        MockMultipartFile mf = new MockMultipartFile("file", "hello.txt", "text/plain", content);

        // 上传
        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                        .file(mf)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("hello.txt"))
                .andReturn();
        Long id = ((Number) JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.data.id")).longValue();
        createdFileIds.add(id);

        // 下载内容一致
        mockMvc.perform(get("/api/files/" + id + "/download").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));

        // 分页列表
        mockMvc.perform(get("/api/files").param("fileName", "hello").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        // 删除
        mockMvc.perform(delete("/api/files/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 删除后下载 → 404
        mockMvc.perform(get("/api/files/" + id + "/download").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void unauthenticatedUploadShouldReturn401() throws Exception {
        MockMultipartFile mf = new MockMultipartFile("file", "a.png", "image/png", "x".getBytes());
        mockMvc.perform(multipart("/api/files/upload").file(mf))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unsupportedTypeUploadShouldReturn400() throws Exception {
        String token = registerAndLogin();
        MockMultipartFile mf = new MockMultipartFile("file", "a.exe", "application/octet-stream", "x".getBytes());
        mockMvc.perform(multipart("/api/files/upload").file(mf).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
