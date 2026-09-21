package com.dyh.club.lock;

public class LockBusyException extends RuntimeException {
    public LockBusyException(String message) { super(message); }
}
