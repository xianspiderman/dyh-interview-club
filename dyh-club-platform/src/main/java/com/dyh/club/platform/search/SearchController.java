package com.dyh.club.platform.search;

import com.dyh.club.platform.common.ApiResponse;
import com.dyh.club.platform.common.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService search; private final CurrentUser user;
    public SearchController(SearchService search,CurrentUser user){this.search=search;this.user=user;}
    @GetMapping public ApiResponse<Map<String,Object>> search(@RequestParam(defaultValue="")String keyword,@RequestParam(required=false)Long categoryId,
        @RequestParam(required=false)Long labelId,@RequestParam(required=false)String type,@RequestParam(defaultValue="relevance")String sort,
        @RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.ok(search.search(keyword,categoryId,labelId,type,sort,page,size));}
    @PostMapping("/rebuild") public ApiResponse<Map<String,String>> rebuild(){user.requireAdmin();return ApiResponse.ok(java.util.Collections.singletonMap("index",search.rebuild()));}
    @PostMapping("/verify") public ApiResponse<Void> verify(){user.requireAdmin();search.verifyAndRepair();return ApiResponse.ok();}
}
