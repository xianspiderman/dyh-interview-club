package com.dyh.club.platform.question;

import com.dyh.club.platform.common.ApiResponse;
import com.dyh.club.platform.common.CurrentUser;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QuestionController {
    private final QuestionService service;
    private final CurrentUser currentUser;

    public QuestionController(QuestionService service, CurrentUser currentUser) { this.service = service; this.currentUser = currentUser; }

    @GetMapping("/questions")
    public ApiResponse<Map<String, Object>> page(@Valid QuestionModels.QuestionQuery query) { return ApiResponse.ok(service.page(query)); }

    @GetMapping("/questions/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long id) {
        boolean includeAnswer = currentUser.optionalId() != null;
        return ApiResponse.ok(service.detail(id, includeAnswer));
    }

    @GetMapping("/catalog/categories")
    public ApiResponse<List<Map<String, Object>>> categories(@RequestParam(required = false) Long parentId) { return ApiResponse.ok(service.categories(parentId)); }

    @GetMapping("/catalog/aggregate")
    public ApiResponse<List<QuestionModels.AggregateResponse>> aggregate(@RequestParam long parentId) { return ApiResponse.ok(service.aggregate(parentId)); }

    @PostMapping("/admin/questions")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody QuestionModels.QuestionRequest request) {
        currentUser.requireAdmin();
        return ApiResponse.ok(java.util.Collections.singletonMap("id", service.create(request, currentUser.id())));
    }

    @PutMapping("/admin/questions/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @Valid @RequestBody QuestionModels.QuestionRequest request) {
        currentUser.requireAdmin(); service.update(id, request); return ApiResponse.ok();
    }

    @PostMapping("/admin/questions/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable long id) { currentUser.requireAdmin(); service.changeStatus(id, "PUBLISHED"); return ApiResponse.ok(); }

    @PostMapping("/admin/questions/{id}/offline")
    public ApiResponse<Void> offline(@PathVariable long id) { currentUser.requireAdmin(); service.changeStatus(id, "OFFLINE"); return ApiResponse.ok(); }
}
