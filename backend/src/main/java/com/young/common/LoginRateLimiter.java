package com.young.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录/注册失败限流器（内存滑动窗口）
 * 支持按账号和 IP 双维度限流，防止暴力破解与密码喷洒
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;
    // 单 IP 在窗口期内最多失败 20 次，防止密码喷洒攻击
    private static final int MAX_IP_FAILURES = 20;

    private final Map<String, FailureRecord> failures = new ConcurrentHashMap<>();
    private final Map<String, FailureRecord> ipFailures = new ConcurrentHashMap<>();

    /**
     * 指定账号当前是否处于限流锁定状态
     */
    public boolean isBlocked(String username) {
        return isBlocked(failures, username);
    }

    /**
     * 指定 IP 当前是否处于限流锁定状态
     */
    public boolean isIpBlocked(String ip) {
        return isBlocked(ipFailures, ip);
    }

    private boolean isBlocked(Map<String, FailureRecord> store, String key) {
        if (key == null) {
            return false;
        }
        FailureRecord record = store.get(key);
        if (record == null) {
            return false;
        }
        if (System.currentTimeMillis() - record.firstFailureTime > WINDOW_MILLIS) {
            store.remove(key);
            return false;
        }
        return record.count >= record.threshold;
    }

    /**
     * 记录一次登录失败（账号 + IP 双维度）
     */
    public void recordFailure(String username, String ip) {
        if (username != null) {
            failures.compute(username, (key, record) -> updateRecord(record, MAX_FAILURES));
        }
        if (ip != null) {
            ipFailures.compute(ip, (key, record) -> updateRecord(record, MAX_IP_FAILURES));
        }
    }

    private FailureRecord updateRecord(FailureRecord record, int threshold) {
        long now = System.currentTimeMillis();
        if (record == null || now - record.firstFailureTime > WINDOW_MILLIS) {
            return new FailureRecord(now, 1, threshold);
        }
        record.count++;
        return record;
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
        private final int threshold;
        private int count;

        FailureRecord(long firstFailureTime, int count, int threshold) {
            this.firstFailureTime = firstFailureTime;
            this.count = count;
            this.threshold = threshold;
        }
    }
}
