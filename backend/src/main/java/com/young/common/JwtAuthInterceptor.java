package com.young.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.young.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthInterceptor.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims = jwtUtils.parseToken(token);
            if (claims != null) {
                // 校验 token 是否已被吊销（登出/改密）
                String jti = claims.getId();
                if (jwtUtils.isRevoked(jti)) {
                    log.warn("[鉴权] Token 已被吊销，IP={}, 路径={}", getClientIp(request), request.getRequestURI());
                    return writeUnauthorized(response, "身份凭证已失效，请重新登录");
                }
                request.setAttribute("userId", Long.parseLong(claims.getSubject()));
                request.setAttribute("role", claims.get("role", Integer.class));
                request.setAttribute("jti", jti);
                if (claims.getExpiration() != null) {
                    request.setAttribute("exp", claims.getExpiration().getTime());
                }
                return true;
            }
        }

        log.warn("[鉴权] 无有效 Token 访问被拒，IP={}, 路径={}", getClientIp(request), request.getRequestURI());
        return writeUnauthorized(response, "无权访问，请先登录以获取合法身份凭证");
    }

    private boolean writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(401);
        Result<?> errorResult = Result.error(401, msg);
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResult));
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        // 优先从可信代理头取真实 IP（注意：X-Forwarded-For 可被伪造，生产应仅信任已知代理）
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty() && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
