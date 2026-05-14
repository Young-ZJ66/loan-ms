package com.young.config;

import com.young.common.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器并开放登录/注册/静态等公共接口
        registry.addInterceptor(new JwtAuthInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/error"
                );
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        // 将前端请求中的 /uploads/** 映射到项目根目录下的 uploads 物理文件夹
        String path = System.getProperty("user.dir") + "/uploads/";
        // 在 Windows 环境下，Spring 的 file: 协议需要统一为正斜杠，并且如果是通过 user.dir 拿到的路径，前面推荐补齐 file:
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path);
    }
}
