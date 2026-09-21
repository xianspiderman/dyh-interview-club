# 生产型高可用拓扑参考

本目录与根目录本地演示 Compose 分离，不把单机 Docker Desktop 描述成生产集群。

- Redis：一主两从、三 Sentinel，法定票数 2。
- RocketMQ：双 NameServer，同 brokerName 的同步主从，主节点 `SYNC_MASTER`，主从均 `SYNC_FLUSH`。
- Nacos：三节点连接独立 MySQL；部署前须导入对应 Nacos 2.4 官方 MySQL schema。
- Elasticsearch：三节点、IK 插件；业务索引由应用固定创建为 1 主分片、1 副本。

静态校验：

```powershell
docker compose -f deploy/ha/compose-ha.yaml config --quiet
```

示例默认值只用于隔离的架构演练。实际部署必须通过环境变量或秘密管理系统替换数据库密码、Nacos 身份密钥和认证 Token，并补齐 TLS、持久卷、反亲和、备份、监控和告警。
