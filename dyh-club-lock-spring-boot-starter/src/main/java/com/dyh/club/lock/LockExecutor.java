package com.dyh.club.lock;

public interface LockExecutor {
    boolean tryLock(String key, String owner, long waitMillis, long leaseMillis);
    boolean unlock(String key, String owner);
}
