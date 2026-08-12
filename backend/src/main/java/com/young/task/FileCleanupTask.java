package com.young.task;

import com.young.mapper.UserProfileMapper;
import com.young.pojo.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定时清理 uploads 目录下的孤儿文件
 */
@Slf4j
@Component
public class FileCleanupTask {

    @Autowired
    private UserProfileMapper profileMapper;

    @Value("${upload.dir}")
    private String uploadDir;

    // 单机运行锁，防止多实例重复执行（多实例部署需替换为分布式锁）
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOrphanedFiles() {
        if (!running.compareAndSet(false, true)) {
            log.warn("文件清理任务已在运行，本次跳过");
            return;
        }
        try {
            doCleanup();
        } finally {
            running.set(false);
        }
    }

    private void doCleanup() {
        log.info("开始执行冗余文件清理任务...");
        File dir = new File(uploadDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        // 收集数据库中已引用的文件名（上传后返回 /api/file/{filename}，存储相对路径）
        List<UserProfile> profiles = profileMapper.selectAllList();
        Set<String> usedFileNames = new HashSet<>();
        for (UserProfile profile : profiles) {
            collectFileName(profile.getIdCardFront(), usedFileNames);
            collectFileName(profile.getIdCardBack(), usedFileNames);
        }

        File[] files = dir.listFiles();
        if (files == null)
            return;

        long now = System.currentTimeMillis();
        int deletedCount = 0;

        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                // 清理未被引用且超过24小时的文件
                if (!usedFileNames.contains(fileName)) {
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

    /**
     * 从存储路径中提取文件名（兼容 /uploads/xxx 与 /api/file/xxx 两种历史格式）
     */
    private void collectFileName(String storedPath, Set<String> usedFileNames) {
        if (storedPath == null || storedPath.isEmpty()) {
            return;
        }
        int slashIdx = storedPath.lastIndexOf('/');
        String fileName = slashIdx >= 0 ? storedPath.substring(slashIdx + 1) : storedPath;
        if (!fileName.isEmpty()) {
            usedFileNames.add(fileName);
        }
    }
}
