param([string]$BaseUrl="http://localhost:5000",[int]$Requests=1000,[int]$Concurrency=100)
$samples=1..$Requests | ForEach-Object -Parallel {
  $watch=[System.Diagnostics.Stopwatch]::StartNew()
  $failed=$false
  try { Invoke-WebRequest "$using:BaseUrl/api/catalog/aggregate?parentId=1" -UseBasicParsing | Out-Null } catch { $failed=$true }
  $watch.Stop(); [pscustomobject]@{Ms=$watch.Elapsed.TotalMilliseconds;Failed=$failed}
} -ThrottleLimit $Concurrency
$errors=($samples | Where-Object Failed).Count
$times=$samples.Ms
$ordered=$times | Sort-Object
$avg=($times | Measure-Object -Average).Average
$p95=$ordered[[Math]::Min($ordered.Count-1,[Math]::Floor($ordered.Count*.95))]
[pscustomobject]@{Requests=$Requests;Concurrency=$Concurrency;AverageMs=[Math]::Round($avg,2);P95Ms=[Math]::Round($p95,2);Errors=$errors;ErrorRate=[Math]::Round($errors*100/$Requests,3)}
