package com.young.common;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限校验切面，拦截 @RequireRole 注解标注的方法
 * 方法级注解优先；方法无注解时回退到类级注解
 */
@Aspect
@Component
public class RequireRoleAspect {

    @Autowired
    private HttpServletRequest request;

    @Around("@within(requireRole) || @annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        // 优先使用方法级注解，回退到类级注解
        RequireRole effective = resolveAnnotation(joinPoint, requireRole);
        int requiredRole = effective != null ? effective.role() : 1;

        Integer role = (Integer) request.getAttribute("role");

        if (role == null || role != requiredRole) {
            throw new BusinessException(403, "权限不足：此操作需要" + (requiredRole == 1 ? "管理员" : "客户") + "身份");
        }

        return joinPoint.proceed();
    }

    private RequireRole resolveAnnotation(ProceedingJoinPoint joinPoint, RequireRole paramAnnotation) {
        if (paramAnnotation != null) {
            return paramAnnotation;
        }
        // 回退到方法签名上的注解
        if (joinPoint.getSignature() instanceof MethodSignature ms) {
            Method method = ms.getMethod();
            RequireRole methodAnnotation = method.getAnnotation(RequireRole.class);
            if (methodAnnotation != null) {
                return methodAnnotation;
            }
            // 回退到类级注解
            RequireRole classAnnotation = method.getDeclaringClass().getAnnotation(RequireRole.class);
            if (classAnnotation != null) {
                return classAnnotation;
            }
        }
        return null;
    }
}
