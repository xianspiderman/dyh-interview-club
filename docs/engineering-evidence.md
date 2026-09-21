# Club 工程证据矩阵

| 能力 | 代码/配置证据 | 自动验证 |
| --- | --- | --- |
| 注册登录、Redis Session、权限 | `auth`、`WebConfig`、Gateway `LoginFilter` | 登录、注册、退出和越权集成测试 |
| 四服务主链路 | `dyh-club-runtime-services` 四个启动器、Gateway 精确路由、练题 Feign 内部接口 | 四个独立上下文测试、`RouteTopologyTest`、`main-chain-smoke.ps1` |
| 题目 CRUD 与四类题型 | `question` 工厂/策略、管理接口、Flyway 明细表 | `QuestionTypeHandlerTest`、初始化数据验证 |
| 分类标签并发聚合 | `QuestionService.aggregate`、`AsyncConfig` | 分支/总超时与降级字段 |
| 两级缓存一致性 | `QuestionCache`、`CacheInvalidationService`、Redis Pub/Sub | 缓存对象隔离、CRUD 与失效任务测试 |
| 专项练习闭环 | `PracticeService`、中文练习页 | 进度保存与重复交卷测试 |
| 评论与多级回复 | `CommunityService` 窗口函数、游标分页 | 3 条预览与 20 条懒加载测试 |
| 敏感词治理 | 不可变 DFA 快照、黑白名单、规范化、复检游标 | 热更新与干扰字符测试 |
| 点赞最终一致 | MySQL 单调版本分配、Redis Lua 版本屏障、RocketMQ、pending 时长、补偿与受控恢复 | 数据库高版本/Redis 丢失、版本 7 先到而版本 6 后到、重复/乱序、降级状态测试 |
| 搜索同步与恢复 | ES 集群节点轮询、IK、`name.keyword`、`createdAt(date)`、名称 2 倍权重、500 条 Bulk、逐项失败重试、Canal checkpoint、近期校验、增量重放与别名切换 | Mapping、查询权重/排序/高亮、节点列表、Bulk 失败项、批量上限和 MySQL 降级测试 |
| 分布式锁 Starter | `spring.factories`、属性、条件 Bean、AOP、Lua | `RedisLockExecutorTest` |
| 网关两层限流 | 全局和访问者两层 Redis 令牌桶 Lua、风险分级故障策略、中文 429/503 | Lua 结构和登录/搜索/交卷/点赞固定参数测试 |
| 生产型中间件高可用部署 | `deploy/ha` 的 Redis Sentinel、RocketMQ 同步主从、Nacos 三节点及幂等 schema 初始化、ES 三节点 | Gateway/运行时 Sentinel 上下文测试、Nacos 表核对命令、独立 Compose 配置验证与端到端冒烟脚本 |
| 可观测性 | Micrometer Filter、Actuator、`OpsDashboardService` | 面板端到端测试 |

Starter 使用 Spring Boot 2.4.2 的 `spring.factories` 完成自动装配，Java SPI 用于解释两种发现机制的边界；容量数字均明确标注压测口径。
