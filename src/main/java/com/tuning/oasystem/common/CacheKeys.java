package com.tuning.oasystem.common;

/**
 * Redis 缓存 key 规范与默认 TTL（秒）。
 * <p>
 * 统一前缀 {@code oa:}；冒号分隔业务域。key 规范详见 dev.md 阶段 4 说明。
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** 登录态：{@code oa:auth:token:{token}} → 用户ID（登出删除，token 立即失效） */
    public static final String AUTH_TOKEN = "oa:auth:token:";

    /** 用户信息（含角色/权限）：{@code oa:user:info:{userId}} → UserInfoVO（供鉴权过滤器 + /me） */
    public static final String USER_INFO = "oa:user:info:";

    /** 用户详情：{@code oa:user:detail:{userId}} → UserVO（热点查询缓存） */
    public static final String USER_DETAIL = "oa:user:detail:";

    /** 菜单树：{@code oa:menu:tree} → MenuVO[]（热点查询缓存） */
    public static final String MENU_TREE = "oa:menu:tree";

    /** 登录态 / 用户信息 TTL：与 JWT 有效期对齐（30 天），由 jwt.expiration 传入 */
    public static final long TTL_TOKEN_DEFAULT = 2592000L;

    /** 用户详情 TTL：10 分钟（用户资料可能变更，短 TTL 兜底） */
    public static final long TTL_USER_DETAIL = 600L;

    /** 菜单树 TTL：30 分钟（菜单低频变更，写操作即时失效兜底） */
    public static final long TTL_MENU_TREE = 1800L;
}
