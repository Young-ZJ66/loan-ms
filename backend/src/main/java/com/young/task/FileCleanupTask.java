package com.young.task;

import com.young.mapper.UserProfileMapper;
import com.young.pojo.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class FileCleanupTask {

    @Autowired
    private UserProfileMapper profileMapper;

    /**
     * 每天凌晨2点执行一次：清理 /uploads 目录下多余的冗余图片
     * （防呆机制：仅清理未被数据库关联且存活时间超过 24 小时的临时文件）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOrphanedFiles() {
        log.info("开始执行磁盘归档冗余文件自检清理任务...");
        String destDirPath = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(destDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        // 1. 获取数据库中所有已关联的文件全路径名
        List<UserProfile> profiles = profileMapper.selectAllList();
        Set<String> usedFiles = new HashSet<>();
        for (UserProfile profile : profiles) {
            if (profile.getIdCardFront() != null)
                usedFiles.add(profile.getIdCardFront());
            if (profile.getIdCardBack() != null)
                usedFiles.add(profile.getIdCardBack());
        }

        // 2. 遍历物理硬盘的实际文件
        File[] files = dir.listFiles();
        if (files == null)
            return;

        long now = System.currentTimeMillis();
        int deletedCount = 0;

        for (File file : files) {
            if (file.isFile()) {
                // 拼接数据库中存储的相对路径格式："/uploads/xxxx.jpg"
                String relativePath = "/uploads/" + file.getName();

                // 如果文件未被业务记录引用，并且上传时间在 24 小时之前，则作为闲置赘余数据硬删除
                if (!usedFiles.contains(relativePath)) {
                    if (now - file.lastModified() > 24 * 60 * 60 * 1000L) {
                        if (file.delete()) {
                            deletedCount++;
                            log.debug("成功抛弃过期的脏数据切片: {}", file.getName());
                        }
                    }
                }
            }
        }
        log.info("冗余文件清理执行完毕，本次共释放销毁了 {} 个废弃图像。", deletedCount);
    }
}
