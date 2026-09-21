package com.dyh.club.runtime.circle;

import com.dyh.club.platform.admin.AdminService;
import com.dyh.club.platform.common.ApiResponse;
import com.dyh.club.platform.common.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class CircleAdminController {
    private final AdminService admin;
    private final CurrentUser user;

    public CircleAdminController(AdminService admin, CurrentUser user) {
        this.admin = admin;
        this.user = user;
    }

    @GetMapping("/posts")
    public ApiResponse<List<Map<String, Object>>> posts() {
        user.requireAdmin();
        return ApiResponse.ok(admin.posts());
    }

    @GetMapping("/comments")
    public ApiResponse<List<Map<String, Object>>> comments() {
        user.requireAdmin();
        return ApiResponse.ok(admin.comments());
    }

    @PutMapping("/{target:post|comment}/{id}/status")
    public ApiResponse<Void> moderate(@PathVariable String target, @PathVariable long id,
                                      @RequestBody Map<String, String> body) {
        user.requireAdmin();
        admin.moderate(target, id, body.get("status"));
        return ApiResponse.ok();
    }
}
