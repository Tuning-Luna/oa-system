package com.tuning.oasystem.service;

import com.tuning.oasystem.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redis 操作封装测试（需 Redis 运行）：set/get 往返、TTL 过期、delete、按模式删除。
 */
@SpringBootTest
class RedisServiceTest {

    private static final String KEY = "oa:test:redis";

    @Autowired
    private RedisService redisService;

    @AfterEach
    void cleanUp() {
        redisService.delete(KEY);
        redisService.delete(KEY + ":1");
        redisService.delete(KEY + ":2");
    }

    @Test
    void setGetShouldRoundtrip() {
        UserVO vo = new UserVO();
        vo.setId(1L);
        vo.setUsername("redis_user");
        vo.setNickname("缓存测试");

        redisService.set(KEY, vo, 60);

        UserVO back = redisService.get(KEY, UserVO.class);
        assertEquals(1L, back.getId());
        assertEquals("redis_user", back.getUsername());
        assertEquals("缓存测试", back.getNickname());
    }

    @Test
    void getMissingKeyShouldReturnNull() {
        assertNull(redisService.get(KEY, UserVO.class));
    }

    @Test
    void ttlShouldExpire() throws InterruptedException {
        redisService.set(KEY, "expire_me", 1);
        assertTrue(redisService.hasKey(KEY));
        Thread.sleep(1300);
        assertFalse(redisService.hasKey(KEY), "TTL 过期后 key 应消失");
    }

    @Test
    void deleteShouldRemoveKey() {
        redisService.set(KEY, "x", 60);
        assertTrue(redisService.hasKey(KEY));
        redisService.delete(KEY);
        assertFalse(redisService.hasKey(KEY));
    }

    @Test
    void deleteByPatternShouldRemoveMatches() {
        redisService.set(KEY + ":1", "a", 60);
        redisService.set(KEY + ":2", "b", 60);

        redisService.deleteByPattern(KEY + ":*");

        assertFalse(redisService.hasKey(KEY + ":1"));
        assertFalse(redisService.hasKey(KEY + ":2"));
    }
}
