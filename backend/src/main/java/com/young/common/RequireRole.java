package com.young.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解，可标注在 Controller 类或方法上
 * 方法级注解优先于类级注解；未标注的方法回退到类级默认值
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色：0-客户, 1-管理员
     */
    int role() default 1;
}
