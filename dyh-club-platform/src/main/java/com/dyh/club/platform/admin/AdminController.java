package com.dyh.club.platform.admin;
import com.dyh.club.platform.common.*;
import com.dyh.club.platform.like.LikeService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin")
public class AdminController{private final AdminService admin;private final CurrentUser user;private final LikeService likes;public AdminController(AdminService admin,CurrentUser user,LikeService likes){this.admin=admin;this.user=user;this.likes=likes;}
@GetMapping("/overview")public ApiResponse<Map<String,Object>>overview(){user.requireAdmin();return ApiResponse.ok(admin.overview());}
@GetMapping("/categories")public ApiResponse<List<Map<String,Object>>>categories(){user.requireAdmin();return ApiResponse.ok(admin.categories());}
@PostMapping("/categories")public ApiResponse<Map<String,Long>>category(@RequestBody Category r){user.requireAdmin();return ApiResponse.ok(Collections.singletonMap("id",admin.saveCategory(r.id,r.parentId,r.name,r.sortNo,r.status)));}
@GetMapping("/labels")public ApiResponse<List<Map<String,Object>>>labels(){user.requireAdmin();return ApiResponse.ok(admin.labels());}
@PostMapping("/labels")public ApiResponse<Map<String,Long>>label(@RequestBody Label r){user.requireAdmin();return ApiResponse.ok(Collections.singletonMap("id",admin.saveLabel(r.id,r.categoryId,r.name,r.status)));}
@GetMapping("/question-types")public ApiResponse<List<Map<String,Object>>>types(){user.requireAdmin();return ApiResponse.ok(admin.types());}
@GetMapping("/posts")public ApiResponse<List<Map<String,Object>>>posts(){user.requireAdmin();return ApiResponse.ok(admin.posts());}
@GetMapping("/comments")public ApiResponse<List<Map<String,Object>>>comments(){user.requireAdmin();return ApiResponse.ok(admin.comments());}
@PutMapping("/{target}/{id}/status")public ApiResponse<Void>moderate(@PathVariable String target,@PathVariable long id,@RequestBody Map<String,String>body){user.requireAdmin();admin.moderate(target,id,body.get("status"));return ApiResponse.ok();}
@PostMapping("/likes/recover")public ApiResponse<Map<String,Integer>>recoverLikes(@RequestParam(defaultValue="100")int limit){user.requireAdmin();return ApiResponse.ok(Collections.singletonMap("processed",likes.recover(limit)));}
public static class Category{public Long id;public Long parentId;public String name;public Integer sortNo;public String status;}public static class Label{public Long id;public long categoryId;public String name;public String status;}}
