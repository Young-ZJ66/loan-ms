package com.young.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import com.young.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Api(tags = "文件上传管理")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @ApiOperation("上传证件影像文件")
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty())
            return Result.error("文件不能为空");

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";

        String newName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 统一存放在项目根目录外的 uploads 目录下，方便静态资源代理和持久化
        String destDirPath = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(destDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            file.transferTo(new File(destDirPath + newName));
            // spring.web.resources.static-locations 会代理 /uploads/** 指向实际路劲
            return Result.success("/uploads/" + newName);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }
}
