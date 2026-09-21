package com.dyh.club.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "club.lock")
public class LockProperties {
    private boolean enabled = true;
    private String keyPrefix = "club:lock:";
    private long waitMillis = 200L;
    private long leaseMillis = 5000L;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public long getWaitMillis() { return waitMillis; }
    public void setWaitMillis(long waitMillis) { this.waitMillis = waitMillis; }
    public long getLeaseMillis() { return leaseMillis; }
    public void setLeaseMillis(long leaseMillis) { this.leaseMillis = leaseMillis; }
}
