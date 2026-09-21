# 运维与故障演练

## 健康检查

```powershell
Invoke-RestMethod http://localhost:3021/actuator/health
Invoke-RestMethod http://localhost:3022/actuator/health
Invoke-RestMethod http://localhost:3023/actuator/health
Invoke-RestMethod http://localhost:3024/actuator/health
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
3. `POST /api/search/rebuild` 记录起点 binlog/checkpoint，创建时间戳索引，分批写入后重放期间增量并校验，最后原子切换别名。切换和 Canal 写批次共用 `search:cutover:all` 分布式锁，单进程内再使用公平读写锁封闭最终重放窗口；旧索引需保留到观察期结束。
4. 如新索引异常，使用 Elasticsearch `_aliases` 将 `club-subject-search` 切回旧索引，再定位映射或数据问题。

### Canal 重复与乱序

Canal 只传题目 ID，消费者每批最多 500 个 ID，从 MySQL 批量读取当前快照并 Bulk 覆盖或删除 ES 文档；重复事件不会回滚文档。Bulk 逐项检查并只重试失败 ID，全部成功后才更新 checkpoint 并 ack；异常时 rollback，错误记录在 `club_search_checkpoint`。

### 多实例检查

- 两个实例使用同一 Redis 后登录，确认 token 可跨实例访问。
- 更新题目后，两实例 Caffeine 均因 Pub/Sub 失效。
- 缓存重建锁有唯一 owner 和 5 秒租期，Lua 只允许 owner 解锁。
- 调度任务使用同一分布式锁；MQ 同业务键固定队列，消费者再以版本兜底。
- TraceId 和用户上下文在聚合线程池中复制，并在 `finally` 中恢复/清理。

## 恢复原则

MySQL 永远是最终数据依据。Redis 可重建，Elasticsearch 可通过 MySQL 全量重建加 Canal 增量重放恢复；RocketMQ 和 pending 用于缩短一致性窗口，不能绕开数据库唯一约束和条件更新。

## 部署模式边界

`compose.yaml` 用于本地演示，组件可按需精简，不宣称生产高可用。`deploy/ha/compose-ha.yaml` 是生产型拓扑证据：Redis 一主两从和三 Sentinel、RocketMQ 双 NameServer 与同步主从/同步刷盘、Nacos 三节点以及 Elasticsearch 三节点。该参考在真实环境使用前必须接入持久卷、TLS、监控、备份和外部秘密管理，并初始化 Nacos 官方数据库 schema。
