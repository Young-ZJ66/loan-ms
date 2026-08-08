package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.young.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Tag(name = "文件上传管理")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final List<String> ALLOWED_EXT = List.of(".jpg", ".jpeg", ".png", ".gif");

    @Operation(summary = "上传证件影像文件")
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
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

        String destDirPath = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(destDirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("创建上传目录失败: {}", destDirPath);
            return Result.error("图片上传失败：无法创建上传目录");
        }

        try {
            file.transferTo(new File(destDirPath + newName));
            return Result.success("/uploads/" + newName);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.error("图片上传失败：" + e.getMessage());
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
            log.error("读取上传文件内容失败", e);
            return false;
        }
    }
}
