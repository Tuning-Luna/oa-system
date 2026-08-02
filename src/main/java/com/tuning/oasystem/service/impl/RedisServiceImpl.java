package com.tuning.oasystem.service.impl;

import com.tuning.oasystem.common.ResultCode;
import com.tuning.oasystem.exception.BusinessException;
import com.tuning.oasystem.service.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis 操作封装实现：值以 JSON 字符串存储（String 序列化 + Jackson 3 ObjectMapper 互转）。
 */
@Service
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final tools.jackson.databind.ObjectMapper objectMapper;

    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate,
            tools.jackson.databind.ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void set(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value),
                    Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "缓存序列化失败: " + key);
        }
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        Object raw = redisTemplate.opsForValue().get(key);
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.readValue(raw.toString(), type);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "缓存反序列化失败: " + key);
        }
    }

    @Override
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    @Override
    public Boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public Boolean expire(String key, long ttlSeconds) {
        return redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
