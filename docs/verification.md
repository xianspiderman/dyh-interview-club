# 工程验证记录

验证日期：2026-09-21。本文只记录本次仓库可以复现的结果，不把容量推演、测试数据或未启动的外部服务描述为生产事实。

## 已通过

- 全仓 35 个 Maven reactor 模块完成编译和测试，`BUILD SUCCESS`。
- 核心模块 14 项测试通过，锁 Starter 2 项测试通过；覆盖 Flyway、注册/登录/退出/权限、题目 CRUD 与状态流转、四题型策略、缓存对象隔离与失效任务、练习保存/恢复/重复交卷、评论预览与回复分页、点赞状态版本、敏感词热更新、搜索降级、线程上下文和真实运维面板。
- Vue 3 前端完成 `typecheck`、ESLint 和生产构建。
- 基础与 full profile 的 Compose 配置均通过静态解析；环境变量、依赖关系、健康检查与镜像声明有效。
- 密钥模式、私钥头、硬编码令牌、构建产物、无效按钮和 TODO/FIXME 已静态检查；旧工具不再把动态生成的私钥、明文或密文输出到控制台。
- Docker Desktop 客户端和 Compose 可用，但本机 Docker Linux Engine 在验证窗口内未响应 API，因此没有把本轮容器启动标成成功；数据库迁移与接口冒烟改由 H2 集成环境实际执行。

## 可重复命令

```powershell
mvn test
cd dyh-club-web
npm ci
npm run typecheck
npm run lint
npm run build
cd ..
docker compose config --quiet
docker compose --profile full config --quiet
```

外部中间件故障路径由禁用开关、MySQL 受限降级、Redis/消息 pending、数据库状态版本、任务记录和管理员受控恢复接口验证。Docker Engine 恢复后可按 README 一条命令完成真实容器联调。
