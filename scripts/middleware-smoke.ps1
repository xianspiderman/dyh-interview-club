param([switch]$Full)
$ErrorActionPreference = 'Stop'
docker info *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker Engine 不可用，未执行真实中间件联调。' }
if ($Full) {
  $env:ROCKETMQ_ENABLED='true'; $env:ELASTICSEARCH_ENABLED='true'; $env:CANAL_ENABLED='true';$env:NACOS_DISCOVERY_ENABLED='true';$env:NACOS_CONFIG_ENABLED='true'
  $env:CLUB_AUTH_URI='lb://club-auth-service';$env:CLUB_SUBJECT_URI='lb://club-subject-service';$env:CLUB_PRACTICE_URI='lb://club-practice-service';$env:CLUB_CIRCLE_URI='lb://club-circle-service'
  docker compose --profile full up -d --build mysql redis nacos namesrv broker es01 es02 es03 canal
  if ($LASTEXITCODE -ne 0) { throw '中间件容器启动失败' }
  $ready=$false;1..30 | ForEach-Object {if(-not $ready){try{Invoke-RestMethod 'http://localhost:8848/nacos/v1/console/health/readiness' | Out-Null;$ready=$true}catch{Start-Sleep -Seconds 2}}};if(-not $ready){throw 'Nacos 未在 60 秒内就绪'}
  docker compose --profile full up -d --build auth-service subject-service practice-service circle-service gateway web
} else {
  docker compose up -d --build mysql redis auth-service subject-service practice-service circle-service gateway web
}
if ($LASTEXITCODE -ne 0) { throw '容器启动失败' }
docker compose ps
docker exec dyh-club-redis redis-cli PING | Select-String PONG | Out-Null
& "$PSScriptRoot/main-chain-smoke.ps1"
$codes=1..6 | ForEach-Object { curl.exe -s -o NUL -w '%{http_code}' -H 'X-Forwarded-For: 198.51.100.77' -H 'Content-Type: application/json' -d '{"username":"nobody","password":"WrongPass123"}' http://localhost:5000/api/auth/login }
if ($codes[-1] -ne '429') { throw "Redis 访问者令牌桶未在第 6 次登录请求限流，状态码=$($codes -join ',')" }
if ($Full) {
  Invoke-RestMethod 'http://localhost:8848/nacos/v1/console/health/readiness' | Out-Null
  $plugins=Invoke-RestMethod 'http://localhost:9200/_cat/plugins?format=json';if(-not ($plugins.component -contains 'analysis-ik')){throw 'Elasticsearch IK 插件未加载'}
  $analysis=Invoke-RestMethod -Method Post -Uri 'http://localhost:9200/_analyze' -ContentType 'application/json' -Body '{"analyzer":"ik_smart","text":"面试刷题平台"}';if(-not $analysis.tokens){throw 'IK 分词验证失败'}
  $base='http://localhost:5000';$admin=Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body '{"username":"club-admin","password":"Club@123"}';$adminHeaders=@{satoken=$admin.data.tokenValue}
  Invoke-RestMethod -Method Post -Uri "$base/api/search/verify" -Headers $adminHeaders | Out-Null
  $user=Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body '{"username":"club-user","password":"Club@123"}';$userHeaders=@{satoken=$user.data.tokenValue}
  $question=(Invoke-RestMethod "$base/api/questions?page=1&size=1").data.records[0].id
  Invoke-RestMethod -Method Post -Uri "$base/api/questions/$question/like" -Headers $userHeaders -ContentType 'application/json' -Body '{"liked":true}' | Out-Null
  $dbPassword=if($env:MYSQL_PASSWORD){$env:MYSQL_PASSWORD}else{'club-local-only'}
  docker exec dyh-club-mysql mysql -udyh_club "-p$dbPassword" dyh_club -e "UPDATE club_question SET data_version=data_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=$question" | Out-Null
  Start-Sleep -Seconds 6
  $document=Invoke-RestMethod "http://localhost:9200/club-subject-search/_doc/$question";if(-not $document.found){throw 'Canal 到 Elasticsearch 的增量文档未找到'}
  $likeVersion=docker exec dyh-club-mysql mysql -N -udyh_club "-p$dbPassword" dyh_club -e "SELECT state_version FROM club_question_like WHERE question_id=$question ORDER BY state_version DESC LIMIT 1";if(-not $likeVersion){throw 'RocketMQ 点赞落库未完成'}
  $running=docker compose ps --status running --services;if($running -notcontains 'broker' -or $running -notcontains 'canal'){throw 'RocketMQ 或 Canal 容器未运行'}
}
Write-Host 'PASS Redis、Gateway、四服务链路及所选中间件真实冒烟完成。'
