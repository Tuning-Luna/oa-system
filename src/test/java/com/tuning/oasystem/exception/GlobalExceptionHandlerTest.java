package com.tuning.oasystem.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理测试：验证业务异常与未知异常均转换为统一结构
 */
@WebMvcTest(TestExceptionController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void businessExceptionShouldReturnUnifiedError() throws Exception {
        mockMvc.perform(get("/test/business-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("业务校验失败"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void unknownExceptionShouldReturnInternalError() throws Exception {
        mockMvc.perform(get("/test/unknown-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统内部错误"));
    }
}
