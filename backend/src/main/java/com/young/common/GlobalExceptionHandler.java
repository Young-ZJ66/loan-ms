package com.young.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public Result<?> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        log.warn("无法找到静态资源或接口: {}", e.getResourcePath());
        return Result.error(404, "找不到该接口或资源");
    }

    /**
     * 捕获业务异常（用户名密码错、账单不存在等），返回 code=400 + 友好消息
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.warn("[业务异常] {}", e.getMessage());
        return Result.error(400, e.getMessage() == null ? "操作失败" : e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统内部异常: ", e);
        return Result.error(500, e.getMessage() == null ? "系统内部错误" : e.getMessage());
    }
}
