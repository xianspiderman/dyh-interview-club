# 生产型中间件高可用部署

本目录提供独立于根目录单机开发环境的中间件高可用部署配置。

- Redis：一主两从、三 Sentinel，法定票数 2。
- RocketMQ：双 NameServer，同 brokerName 的同步主从，主节点 `SYNC_MASTER`，主从均 `SYNC_FLUSH`。
- Nacos：三节点连接独立 MySQL；`nacos-schema-init` 在节点启动前幂等导入 Nacos 2.4 表结构并核对 12 张必需表，三个节点均配置 readiness 健康检查。
- Elasticsearch：三节点、IK 插件；业务索引由应用固定创建为 1 主分片、1 副本。

本地单 Redis 部署使用根目录 `compose.yaml` 和 `REDIS_HOST/REDIS_PORT`，不设置任何 Sentinel 变量。应用部署到本目录 HA 网络时，对 Gateway 与认证、题目、练题、社区服务统一注入 `application-sentinel.env.example` 中的 `SPRING_REDIS_SENTINEL_*` 变量；Spring Boot 会切换为 Sentinel-aware Lettuce 连接工厂。

配置验证：

```powershell
docker compose -f deploy/ha/compose-ha.yaml config --quiet
```

启动并验证 Redis Sentinel 与 Nacos HA（首次运行会创建命名卷并自动初始化 Nacos 数据库）：

```powershell
docker compose -f deploy/ha/compose-ha.yaml up -d redis-master redis-replica-1 redis-replica-2 sentinel-1 sentinel-2 sentinel-3 nacos-mysql nacos-schema-init nacos-1 nacos-2 nacos-3
docker compose -f deploy/ha/compose-ha.yaml ps
docker compose -f deploy/ha/compose-ha.yaml exec -T sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name clubmaster
docker compose -f deploy/ha/compose-ha.yaml exec -T nacos-mysql mysql -unacos -pnacos-ha-local-only -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='nacos' AND table_name IN ('config_info','config_info_aggr','config_info_beta','config_info_tag','config_tags_relation','group_capacity','his_config_info','tenant_capacity','tenant_info','users','roles','permissions')"
Invoke-RestMethod http://localhost:8848/nacos/v1/console/health/readiness
```

表数量应为 `12`，Sentinel 应返回 `redis-master` 与 `6379`，Nacos readiness 应成功。初始化脚本位于 `nacos/mysql-schema.sql`，使用 `CREATE TABLE IF NOT EXISTS`，可以安全重复执行。停止但保留数据使用 `docker compose -f deploy/ha/compose-ha.yaml down`；仅在明确需要清除 HA 数据时追加 `-v`。

示例默认值用于隔离的开发环境。实际部署通过环境变量或秘密管理系统注入数据库密码、Nacos 身份密钥和认证 Token，并启用 TLS、持久卷、反亲和、备份、监控和告警。
