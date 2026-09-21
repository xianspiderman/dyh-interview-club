package com.dyh.club.platform.common;

public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }

    public static BizException badRequest(String message) { return new BizException(400, message); }
    public static BizException forbidden(String message) { return new BizException(403, message); }
    public static BizException notFound(String message) { return new BizException(404, message); }
    public static BizException conflict(String message) { return new BizException(409, message); }
}
