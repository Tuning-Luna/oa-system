package com.tuning.oasystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成 / 解析 / 校验（HS256）。
 * <p>
 * subject 存用户 ID，附加 username 声明；有效期由配置 {@code jwt.expiration}（秒）决定。
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expirationSeconds) {
        // HS256 要求密钥至少 256 bit（32 字节），配置项必须满足
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    /** 生成 token */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 解析并校验签名/有效期，返回 claims；无效或过期抛 {@link JwtException} */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 从 token 中取用户 ID */
    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    /** 校验 token 是否有效 */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Token 有效期（秒），供登录态缓存 TTL 对齐 */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
