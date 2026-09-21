package com.dyh.club.platform.community;

import com.dyh.club.platform.common.ApiResponse;
import com.dyh.club.platform.common.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sensitive-words")
public class SensitiveWordController {
    private final SensitiveWordService service;private final CurrentUser user;
    public SensitiveWordController(SensitiveWordService service,CurrentUser user){this.service=service;this.user=user;}
    @GetMapping public ApiResponse<List<Map<String,Object>>> list(){user.requireAdmin();return ApiResponse.ok(service.listWords());}
    @PostMapping public ApiResponse<Void> save(@RequestBody WordRequest request){user.requireAdmin();service.saveWord(request.word,request.type);return ApiResponse.ok();}
    @DeleteMapping("/{id}") public ApiResponse<Void> disable(@PathVariable long id){user.requireAdmin();service.disableWord(id);return ApiResponse.ok();}
    public static class WordRequest{public String word;public String type;}
}
