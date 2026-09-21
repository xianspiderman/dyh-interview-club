# 核心 API 调用与验证

统一入口为 `http://localhost:5000`，响应统一包含 `code`、`message`、`data`、`traceId`。先登录并保存返回的 `tokenValue`，后续请求使用 `satoken` 请求头。

```powershell
$base = "http://localhost:5000"
$login = Invoke-RestMethod "$base/api/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"club-admin","password":"Club@123"}'
$headers = @{ satoken = $login.data.tokenValue }

# 题库与分类标签聚合
Invoke-RestMethod "$base/api/questions?status=PUBLISHED&page=1&size=20"
Invoke-RestMethod "$base/api/catalog/aggregate?parentId=1"
Invoke-RestMethod "$base/api/search?keyword=Java&page=1&size=20"

# 生成专项练习、读取进度、交卷
$created = Invoke-RestMethod "$base/api/practices" -Method Post -Headers $headers -ContentType "application/json" -Body '{"title":"Java 专项","count":5}'
$practiceId = $created.data.id
Invoke-RestMethod "$base/api/practices/$practiceId" -Headers $headers
Invoke-RestMethod "$base/api/practices/$practiceId/submit" -Method Post -Headers $headers -ContentType "application/json" -Body '{"elapsedSeconds":120}'

# 社区与运维面板
Invoke-RestMethod "$base/api/community/posts?page=1&size=20"
Invoke-RestMethod "$base/api/ops/dashboard"
```

管理接口均执行服务端角色校验。搜索重建、数据校验和点赞受控恢复分别是：

- `POST /api/search/rebuild`
- `POST /api/search/verify`
- `POST /api/admin/likes/recover?limit=100`

这些写操作只应在定位完故障后由管理员执行；前端普通用户页面不暴露危险恢复按钮。
