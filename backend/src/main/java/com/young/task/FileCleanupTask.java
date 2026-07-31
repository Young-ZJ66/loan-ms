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

/**
 * 定时清理 uploads 目录下的孤儿文件
 */
@Slf4j
@Component
public class FileCleanupTask {

    @Autowired
    private UserProfileMapper profileMapper;

    /**
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOrphanedFiles() {
        log.info("开始执行冗余文件清理任务...");
        String destDirPath = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(destDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        // 收集数据库中已引用的文件路径
        List<UserProfile> profiles = profileMapper.selectAllList();
        Set<String> usedFiles = new HashSet<>();
        for (UserProfile profile : profiles) {
            if (profile.getIdCardFront() != null)
                usedFiles.add(profile.getIdCardFront());
            if (profile.getIdCardBack() != null)
                usedFiles.add(profile.getIdCardBack());
        }

        File[] files = dir.listFiles();
        if (files == null)
            return;

        long now = System.currentTimeMillis();
        int deletedCount = 0;

        for (File file : files) {
            if (file.isFile()) {
                String relativePath = "/uploads/" + file.getName();

                // 清理未被引用且超过24小时的文件
                if (!usedFiles.contains(relativePath)) {
                    if (now - file.lastModified() > 24 * 60 * 60 * 1000L) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }
            }
        }
        log.info("冗余文件清理完毕，共删除 {} 个文件", deletedCount);
    }
}
