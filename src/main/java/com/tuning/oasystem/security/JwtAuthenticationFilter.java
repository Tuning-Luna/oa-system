package com.tuning.oasystem.security;

import com.tuning.oasystem.common.CacheKeys;
import com.tuning.oasystem.service.RedisService;
import com.tuning.oasystem.vo.UserInfoVO;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：从 {@code Authorization: Bearer <token>} 解析并校验 token，
 * 成功后把认证信息写入 {@link SecurityContextHolder}；失败则保持未认证，由入口点返回 401。
 * <p>
 * 阶段 4 起接入 Redis：
 * <ul>
 *   <li>校验登录态 key（{@code oa:auth:token:{token}}），登出/被踢后 token 立即失效；</li>
 *   <li>用户信息（含权限）优先读缓存（{@code oa:user:info:{userId}}），miss 才查 DB 并回填。</li>
 * </ul>
 * 注意：本过滤器由 {@code SecurityConfig} 以 {@code @Bean} 方式注册，而非 {@code @Component}，
 * 以避免 @WebMvcTest 切片测试因扫描到 Filter 而引入其依赖。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final RedisService redisService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
            UserDetailsServiceImpl userDetailsService,
            RedisService redisService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long userId = jwtTokenProvider.getUserId(token);
                // 登录态校验：Redis 中不存在该 token 视为已登出/被踢
                if (!Boolean.TRUE.equals(redisService.hasKey(CacheKeys.AUTH_TOKEN + token))) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                LoginUser loginUser = loadLoginUser(userId);
                if (loginUser.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                // token 无效 / 用户不存在 / 用户被删除：清除上下文，保持未认证
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    /** 优先读缓存用户信息，miss 则查 DB 并回填（仅启用用户回填） */
    private LoginUser loadLoginUser(Long userId) {
        UserInfoVO cached = redisService.get(CacheKeys.USER_INFO + userId, UserInfoVO.class);
        if (cached != null && cached.getUser() != null) {
            return LoginUser.fromInfoVO(cached);
        }
        LoginUser loginUser = userDetailsService.loadUserById(userId);
        if (loginUser.isEnabled()) {
            redisService.set(CacheKeys.USER_INFO + userId, loginUser.toInfoVO(),
                    jwtTokenProvider.getExpirationSeconds());
        }
        return loginUser;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
