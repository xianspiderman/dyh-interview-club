package com.dyh.club.platform.ops;
import com.dyh.club.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/ops")
public class OpsController{private final OpsDashboardService ops;public OpsController(OpsDashboardService ops){this.ops=ops;}@GetMapping("/dashboard")public ApiResponse<Map<String,Object>> dashboard(){return ApiResponse.ok(ops.dashboard());}}
