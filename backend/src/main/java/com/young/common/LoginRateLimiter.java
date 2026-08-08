package com.young.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流器（内存滑动窗口）
 * 防止针对账号密码的暴力破解
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;

    private final Map<String, FailureRecord> failures = new ConcurrentHashMap<>();

    /**
     * 指定账号当前是否处于限流锁定状态
     */
    public boolean isBlocked(String username) {
        if (username == null) {
            return false;
        }
        FailureRecord record = failures.get(username);
        if (record == null) {
            return false;
        }
        if (System.currentTimeMillis() - record.firstFailureTime > WINDOW_MILLIS) {
            failures.remove(username);
            return false;
        }
        return record.count >= MAX_FAILURES;
    }

    /**
     * 记录一次登录失败
     */
    public void recordFailure(String username) {
        if (username == null) {
            return;
        }
        failures.compute(username, (key, record) -> {
            long now = System.currentTimeMillis();
            if (record == null || now - record.firstFailureTime > WINDOW_MILLIS) {
                return new FailureRecord(now, 1);
            }
            record.count++;
            return record;
        });
    }

    /**
     * 登录成功后清除失败记录
     */
    public void reset(String username) {
        if (username != null) {
            failures.remove(username);
        }
    }

    private static class FailureRecord {
        private final long firstFailureTime;
        private int count;

        FailureRecord(long firstFailureTime, int count) {
            this.firstFailureTime = firstFailureTime;
            this.count = count;
        }
    }
}
