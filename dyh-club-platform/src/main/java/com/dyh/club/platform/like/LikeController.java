package com.dyh.club.platform.like;

import com.dyh.club.platform.common.ApiResponse;import com.dyh.club.platform.common.CurrentUser;
import org.springframework.web.bind.annotation.*;import java.util.Map;

@RestController
@RequestMapping("/api/questions/{questionId}/like")
public class LikeController {
    private final LikeService service;private final CurrentUser user;
    public LikeController(LikeService service,CurrentUser user){this.service=service;this.user=user;}
    @PutMapping public ApiResponse<Map<String,Object>> set(@PathVariable long questionId,@RequestBody LikeRequest request){return ApiResponse.ok(service.set(questionId,user.id(),request.liked));}
    @GetMapping public ApiResponse<Map<String,Object>> status(@PathVariable long questionId){return ApiResponse.ok(service.status(questionId,user.optionalId()));}
    public static class LikeRequest{public boolean liked;}
}
