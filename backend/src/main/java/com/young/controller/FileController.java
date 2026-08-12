package com.young.controller;

import com.young.common.Result;
import com.young.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 鉴权文件下载接口
 * 证件影像等敏感文件必须通过本接口访问，校验登录身份后方可下载
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${upload.dir}")
    private String uploadDir;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String filename, HttpServletRequest request) {
        // 优先从 Authorization header 取 token，兼容 query 参数 token（用于 img 标签）
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Claims claims = jwtUtils.parseToken(token);
        if (claims == null || jwtUtils.isRevoked(claims.getId())) {
            return ResponseEntity.status(401).build();
        }

        // 防止路径穿越：仅取文件名，拼接上传目录
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetPath = uploadPath.resolve(filename).normalize();
        if (!targetPath.startsWith(uploadPath)) {
            log.warn("[文件下载] 路径穿越拦截，filename={}, IP={}", filename, request.getRemoteAddr());
            return ResponseEntity.status(403).build();
        }
        File file = targetPath.toFile();
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, guessMediaType(filename))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 兼容 img 标签无法设置 header 的场景
        return request.getParameter("token");
    }

    private String guessMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
