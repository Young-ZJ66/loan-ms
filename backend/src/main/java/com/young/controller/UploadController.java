package com.young.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.young.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Tag(name = "文件上传管理")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Operation(summary = "上传证件影像文件")
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty())
            return Result.error("文件不能为空");

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";

        String lowerExt = ext.toLowerCase();
        if (!java.util.List.of(".jpg", ".jpeg", ".png", ".gif").contains(lowerExt)) {
            return Result.error("文件上传失败：不支持的文件类型！仅支持上传 JPG, JPEG, PNG, GIF 格式的图片文件。");
        }

        String newName = UUID.randomUUID().toString().replace("-", "") + lowerExt;

        String destDirPath = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(destDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            file.transferTo(new File(destDirPath + newName));
            return Result.success("/uploads/" + newName);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }
}
