# 工程验证记录

验证日期：2026-09-21。本文只记录本次仓库可以复现的结果，不把容量推演、测试数据或未启动的外部服务描述为生产事实。

## 已通过

- 全仓 40 个 Maven reactor 模块完成 `clean test`，`BUILD SUCCESS`；15 份测试报告共 36 项测试，失败、错误、跳过均为 0。
- 测试覆盖 Flyway、注册/登录/退出/权限、题目 CRUD 与状态流转、四题型策略、缓存对象隔离与失效任务、练习保存/恢复/重复交卷、评论预览与回复分页、数据库高版本后 Redis 丢失恢复、跨实例并发版本分配、版本 7 先到而版本 6 后到的点赞乱序、Gateway/四运行时服务 Sentinel 绑定与单 Redis 回退、敏感词热更新、搜索集群节点/IK/Mapping/查询权重与高亮/Bulk 失败项、线程上下文和真实运维面板。
- 认证、题目、练题、社区四个独立运行时的 Spring 上下文测试均通过；Gateway 路由测试确认主 `/api` 链路不存在 platform 全量兜底。
- Vue 3 前端完成 `npm ci`、`typecheck`、ESLint、生产构建和 `npm audit`，审计结果为 0 漏洞。
- 本地基础、full profile 与独立 HA 参考三套 Compose 配置均通过静态解析；环境变量、依赖关系、健康检查与镜像声明有效。
- 密钥模式、私钥头、硬编码令牌、构建产物、无效按钮和 TODO/FIXME 已静态检查；旧工具不再把动态生成的私钥、明文或密文输出到控制台。
- Docker Desktop 客户端和 Compose 可用，但本机 Docker Linux Engine 在验证窗口内不存在 `dockerDesktopLinuxEngine` 管道，`docker info` 返回失败。因此没有把 Redis、RocketMQ、Elasticsearch、Canal、Nacos 或容器化 Gateway 的真实联调标成成功；Flyway 与业务接口由 H2/MockMvc 实际执行，真实中间件命令保留在 `scripts/middleware-smoke.ps1`，Docker 恢复后可重复运行。

## 可重复命令

```powershell
mvn test
cd dyh-club-web
npm ci
npm run typecheck
npm run lint
npm run build
npm audit
cd ..
docker compose config --quiet
docker compose --profile full config --quiet
docker compose -f deploy/ha/compose-ha.yaml config --quiet
.\scripts\middleware-smoke.ps1
.\scripts\middleware-smoke.ps1 -Full
```

外部中间件故障路径由禁用开关、MySQL 受限降级、Redis/消息 pending、数据库状态版本、任务记录和管理员受控恢复接口验证。冒烟脚本会实际验证 Redis PING/令牌桶、四服务 Gateway 主链路、分布式锁入口、Nacos 发现、RocketMQ 点赞落库、Canal 增量和 Elasticsearch IK 文档。
