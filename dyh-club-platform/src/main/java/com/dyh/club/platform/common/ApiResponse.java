package com.dyh.club.platform.common;

public class ApiResponse<T> {
    private final int code;
    private final String message;
    private final T data;
    private final String traceId;

    private ApiResponse(int code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "操作成功", data, TraceContext.traceId());
    }

    public static ApiResponse<Void> ok() { return ok(null); }

    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null, TraceContext.traceId());
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getTraceId() { return traceId; }
}
