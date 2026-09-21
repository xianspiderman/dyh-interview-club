# 运维与故障演练

## 健康检查

```powershell
Invoke-RestMethod http://localhost:3020/actuator/health
Invoke-RestMethod http://localhost:5000/api/ops/dashboard
docker compose ps
```

面板只读且不返回凭证。Prometheus 指标位于 `/actuator/prometheus`。

## 故障演练

### Redis 与缓存一致性

1. 连续读取同一道题，在面板观察 Caffeine 命中。
2. 修改题目后确认失效任务变为 `DONE`；多个应用实例会收到 Pub/Sub 通知。
3. 暂停 Redis，读取仍回源 MySQL；恢复后重试任务会删除旧缓存。

### 消息失败与补偿

1. 在完整模式暂停 broker 后发起点赞。
2. Redis 的 `club:like:pending` 保留业务键、目标状态和版本。
3. 恢复 broker 或直接执行 `subjectLikeCompensation` handler；数据库只接受更高版本。
4. pending 超过 5000 条或最老记录超过 2 分钟时，技术面板显示积压告警。
5. 死信或异常积压必须先定位根因，再由管理员调用 `POST /api/admin/likes/recover?limit=100` 受控恢复；接口将批量上限硬限制为 100，禁止整批无条件重投。

### 搜索同步、重建和回退

1. 暂停 Elasticsearch 后搜索，确认响应为 `MYSQL_LIMITED` 且第 6 页被拒绝。
2. 恢复后调用管理员 `POST /api/search/verify` 修复漏同步。
3. `POST /api/search/rebuild` 创建时间戳索引，完成后原子切换别名；不要提前删除旧索引。
4. 如新索引异常，使用 Elasticsearch `_aliases` 将 `club-subject-search` 切回旧索引，再定位映射或数据问题。

### Canal 重复与乱序

Canal 只传题目 ID，消费者总是从 MySQL 读取当前完整快照并覆盖 ES 文档；因此重复与旧 binlog 不会把文档回滚。批次中任一文档失败时执行 rollback 而不 ack，位点与错误记录在 `club_search_checkpoint`。

### 多实例检查

- 两个实例使用同一 Redis 后登录，确认 token 可跨实例访问。
- 更新题目后，两实例 Caffeine 均因 Pub/Sub 失效。
- 缓存重建锁有唯一 owner 和 5 秒租期，Lua 只允许 owner 解锁。
- 调度任务使用同一分布式锁；MQ 同业务键固定队列，消费者再以版本兜底。
- TraceId 和用户上下文在聚合线程池中复制，并在 `finally` 中恢复/清理。

## 恢复原则

MySQL 永远是最终数据依据。Redis 可重建，Elasticsearch 可通过 MySQL 全量重建加 Canal 增量重放恢复；RocketMQ 和 pending 用于缩短一致性窗口，不能绕开数据库唯一约束和条件更新。
