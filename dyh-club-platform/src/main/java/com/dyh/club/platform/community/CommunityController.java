package com.dyh.club.platform.community;

import com.dyh.club.platform.common.ApiResponse;
import com.dyh.club.platform.common.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class CommunityController {
    private final CommunityService service;private final CurrentUser user;
    public CommunityController(CommunityService service,CurrentUser user){this.service=service;this.user=user;}
    @GetMapping("/circles") public ApiResponse<List<Map<String,Object>>> circles(){return ApiResponse.ok(service.circles());}
    @GetMapping("/posts") public ApiResponse<Map<String,Object>> posts(@RequestParam(required=false)Long circleId,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.ok(service.posts(circleId,page,size));}
    @GetMapping("/posts/{id}") public ApiResponse<Map<String,Object>> post(@PathVariable long id){return ApiResponse.ok(service.post(id));}
    @PostMapping("/posts") public ApiResponse<Map<String,Long>> createPost(@RequestBody CommunityService.PostRequest request){return ApiResponse.ok(Collections.singletonMap("id",service.createPost(user.id(),request)));}
    @GetMapping("/comments") public ApiResponse<Map<String,Object>> comments(CommunityService.CommentQuery query){return ApiResponse.ok(service.comments(query));}
    @PostMapping("/comments") public ApiResponse<Map<String,Long>> createComment(@RequestBody CommunityService.CommentRequest request){return ApiResponse.ok(Collections.singletonMap("id",service.createComment(user.id(),request)));}
    @GetMapping("/replies") public ApiResponse<Map<String,Object>> replies(@RequestParam long rootId,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(service.replies(rootId,page,size));}
    @DeleteMapping("/comments/{id}") public ApiResponse<Void> delete(@PathVariable long id){service.deleteComment(user.id(),id,user.isAdmin());return ApiResponse.ok();}
}
