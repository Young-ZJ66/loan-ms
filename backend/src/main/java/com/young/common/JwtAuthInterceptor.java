package com.young.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.young.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class JwtAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims = JwtUtils.parseToken(token);
            if (claims != null) {
                // 将解密出的身份信息挂载到 Request，供 Controller 使用
                request.setAttribute("userId", Long.parseLong(claims.getSubject()));
                request.setAttribute("role", claims.get("role"));
                return true;
            }
        }

        // 统一返回 401 拦截格式
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(401);
        Result<?> errorResult = Result.error(401, "无权访问，请先登录以获取合法身份凭证");
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResult));
        return false;
    }
}
