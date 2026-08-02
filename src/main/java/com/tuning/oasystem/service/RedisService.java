package com.tuning.oasystem.service;

/**
 * Redis 操作封装：值以 JSON 字符串存储。
 * <p>
 * 支持 set/get/delete/expire/hasKey，以及按模式批量删除（供写后失效与运维清缓存）。
 */
public interface RedisService {

    /** 写入缓存并设 TTL（秒） */
    void set(String key, Object value, long ttlSeconds);

    /** 读取缓存并按类型反序列化；不存在返回 null */
    <T> T get(String key, Class<T> type);

    /** 删除指定 key */
    Boolean delete(String key);

    /** key 是否存在 */
    Boolean hasKey(String key);

    /** 更新 TTL（秒） */
    Boolean expire(String key, long ttlSeconds);

    /** 按模式删除（如 {@code oa:user:info:*}，走 KEYS 扫描，仅适用于小数据量/开发场景） */
    void deleteByPattern(String pattern);
}
