package com.young.common;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 权限校验切面，拦截 @RequireRole 注解标注的方法
 */
@Aspect
@Component
public class RequireRoleAspect {

    @Autowired
    private HttpServletRequest request;

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        Integer role = (Integer) request.getAttribute("role");
        int requiredRole = requireRole.role();

        if (role == null || role != requiredRole) {
            throw new BusinessException(403, "权限不足：此操作需要管理员身份");
        }

        return joinPoint.proceed();
    }
}
