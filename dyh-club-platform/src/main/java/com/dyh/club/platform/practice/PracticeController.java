package com.dyh.club.platform.practice;

import com.dyh.club.platform.common.ApiResponse;
import com.dyh.club.platform.common.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/practices")
public class PracticeController {
    private final PracticeService service;
    private final CurrentUser user;

    public PracticeController(PracticeService service, CurrentUser user) { this.service = service; this.user = user; }

    @PostMapping
    public ApiResponse<Map<String, Long>> generate(@RequestBody PracticeService.GenerateRequest request) {
        return ApiResponse.ok(Collections.singletonMap("id", service.generate(user.id(), request)));
    }

    @GetMapping("/unfinished")
    public ApiResponse<List<Map<String, Object>>> unfinished() { return ApiResponse.ok(service.unfinished(user.id())); }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> resume(@PathVariable long id) { return ApiResponse.ok(service.resume(user.id(), id)); }

    @PutMapping("/{id}/answers")
    public ApiResponse<Void> answer(@PathVariable long id, @RequestBody PracticeService.AnswerRequest request) {
        service.saveAnswer(user.id(), id, request.questionId, request.answer, request.elapsedSeconds); return ApiResponse.ok();
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Map<String, Object>> submit(@PathVariable long id, @RequestBody PracticeService.SubmitRequest request) {
        return ApiResponse.ok(service.submit(user.id(), id, request.elapsedSeconds));
    }

    @GetMapping("/{id}/report")
    public ApiResponse<Map<String, Object>> report(@PathVariable long id) { return ApiResponse.ok(service.report(user.id(), id)); }
}
