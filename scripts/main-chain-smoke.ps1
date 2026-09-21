$ErrorActionPreference = 'Stop'
$base = if ($env:CLUB_GATEWAY_URL) { $env:CLUB_GATEWAY_URL.TrimEnd('/') } else { 'http://localhost:5000' }
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body '{"username":"club-user","password":"Club@123"}'
$headers = @{ satoken = $login.data.tokenValue }
$questions = Invoke-RestMethod -Uri "$base/api/questions?page=1&size=5"
$practice = Invoke-RestMethod -Method Post -Uri "$base/api/practices" -Headers $headers -ContentType 'application/json' -Body '{"title":"主链路冒烟","count":2}'
$resumed = Invoke-RestMethod -Uri "$base/api/practices/$($practice.data.id)" -Headers $headers
$circles = Invoke-RestMethod -Uri "$base/api/community/circles"
$search = Invoke-RestMethod -Uri "$base/api/search?keyword=Java&page=1&size=5"
if (-not $questions.data.records -or -not $practice.data.id -or -not $resumed.data.questions[0].question -or -not $circles.data -or -not $search.data) { throw '四服务主链路响应不完整' }
Write-Host "PASS auth -> subject -> practice(Feign subject) -> circle -> search; practiceId=$($practice.data.id)"
