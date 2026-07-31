package com.young.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.young.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

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
                request.setAttribute("userId", Long.parseLong(claims.getSubject()));
                request.setAttribute("role", claims.get("role", Integer.class));
                return true;
            }
        }

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(401);
        Result<?> errorResult = Result.error(401, "无权访问，请先登录以获取合法身份凭证");
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResult));
        return false;
    }
}
