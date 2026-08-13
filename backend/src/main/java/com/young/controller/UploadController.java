package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.young.common.BusinessException;
import com.young.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Tag(name = "文件上传管理")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final List<String> ALLOWED_EXT = List.of(".jpg", ".jpeg", ".png", ".gif");
    // 单文件最大 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    @Value("${upload.dir}")
    private String uploadDir;

    @Operation(summary = "上传证件影像文件")
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件过大，最大支持 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";

        if (!ALLOWED_EXT.contains(ext)) {
            return Result.error("文件上传失败：不支持的文件类型！仅支持上传 JPG, JPEG, PNG, GIF 格式的图片文件。");
        }

        // 校验文件真实内容（魔数），防止伪造扩展名
        if (!isValidImage(file)) {
            return Result.error("文件上传失败：文件内容与图片格式不符");
        }

        String newName = UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            Path targetPath = uploadPath.resolve(newName).normalize();
            if (!targetPath.startsWith(uploadPath)) {
                return Result.error("文件上传失败：非法文件名");
            }
            file.transferTo(targetPath.toFile());
            // 返回鉴权下载路径，前端通过该路径携带 token 访问
            return Result.success("/api/file/" + newName);
        } catch (IOException e) {
            log.error("[文件上传] 保存文件失败", e);
            return Result.error("图片上传失败，请稍后重试");
        }
    }

    /**
     * 通过文件头魔数校验真实图片格式
     */
    private boolean isValidImage(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = in.read(header);
            if (read < 3) {
                return false;
            }
            boolean jpeg = (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            boolean png = read >= 4 && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
            boolean gif = read >= 4 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8';
            return jpeg || png || gif;
        } catch (IOException e) {
            log.error("[文件上传] 读取文件内容失败", e);
            return false;
        }
    }
}
