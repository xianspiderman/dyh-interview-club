package com.dyh.club.platform.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.dyh.club.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(service.register(request.username, request.password, request.nickname));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(service.login(request.username, request.password));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        service.logout();
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.ok(service.user(StpUtil.getLoginIdAsLong()));
    }

    public static class RegisterRequest {
        @NotBlank public String username;
        @NotBlank @Size(min = 8, max = 64) public String password;
        @NotBlank @Size(max = 32) public String nickname;
    }

    public static class LoginRequest {
        @NotBlank public String username;
        @NotBlank public String password;
    }
}
