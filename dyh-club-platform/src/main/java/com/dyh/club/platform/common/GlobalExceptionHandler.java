package com.dyh.club.platform.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.dyh.club.lock.LockBusyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> business(BizException error) {
        HttpStatus status;
        try { status = HttpStatus.valueOf(error.getCode()); } catch (IllegalArgumentException invalid) { status = HttpStatus.BAD_REQUEST; }
        return ResponseEntity.status(status).body(ApiResponse.error(error.getCode(), error.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validation(Exception error) {
        String message = "请求参数不合法";
        if (error instanceof MethodArgumentNotValidException
                && ((MethodArgumentNotValidException) error).getBindingResult().getFieldError() != null) {
            message = ((MethodArgumentNotValidException) error).getBindingResult().getFieldError().getDefaultMessage();
        }
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> notLogin(NotLoginException ignored) {
        return ApiResponse.error(401, "请先登录");
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> noPermission(NotPermissionException ignored) {
        return ApiResponse.error(403, "没有操作权限");
    }

    @ExceptionHandler(LockBusyException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> lockBusy(LockBusyException error) {
        return ApiResponse.error(429, error.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> unexpected(Exception error) {
        log.error("未处理异常，traceId={}", TraceContext.traceId(), error);
        return ApiResponse.error(500, "系统暂时不可用，请稍后重试");
    }
}
