package com.tuning.oasystem.common;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一返回结构测试：确保 JSON 序列化结果为 {code, message, data}
 * <p>
 * Spring Boot 4 内置 Jackson 3（tools.jackson 包），此处用其序列化器对齐运行时行为。
 */
class ResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("deprecation")
    @Test
    void successShouldSerializeToExpectedStructure() throws Exception {
        String json = objectMapper.writeValueAsString(Result.success(Map.of("key", "value")));

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("code").asInt()).isEqualTo(200);
        assertThat(node.get("message").asText()).isEqualTo("success");
        assertThat(node.get("data").get("key").asText()).isEqualTo("value");
    }

    @SuppressWarnings("deprecation")
    @Test
    void errorShouldCarryCodeAndMessage() throws Exception {
        String json = objectMapper.writeValueAsString(Result.error(ResultCode.BAD_REQUEST, "参数错误"));

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("code").asInt()).isEqualTo(400);
        assertThat(node.get("message").asText()).isEqualTo("参数错误");
        assertThat(node.get("data").isNull()).isTrue();
    }
}
