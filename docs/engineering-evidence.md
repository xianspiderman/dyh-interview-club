# Club 工程证据矩阵

| 能力 | 代码/配置证据 | 自动验证 |
| --- | --- | --- |
| 注册登录、Redis Session、权限 | `auth`、`WebConfig`、Gateway `LoginFilter` | 登录、注册、退出和越权集成测试 |
| 题目 CRUD 与四类题型 | `question` 工厂/策略、管理接口、Flyway 明细表 | `QuestionTypeHandlerTest`、演示初始化 |
| 分类标签并发聚合 | `QuestionService.aggregate`、`AsyncConfig` | 分支/总超时与降级字段 |
| 两级缓存一致性 | `QuestionCache`、`CacheInvalidationService`、Redis Pub/Sub | 缓存对象隔离、CRUD 与失效任务测试 |
| 专项练习闭环 | `PracticeService`、中文练习页 | 进度保存与重复交卷测试 |
| 评论与多级回复 | `CommunityService` 窗口函数、游标分页 | 3 条预览与 20 条懒加载测试 |
| 敏感词治理 | 不可变 DFA 快照、黑白名单、规范化、复检游标 | 热更新与干扰字符测试 |
| 点赞最终一致 | Redis Lua、RocketMQ、状态版本、pending 时长、补偿与受控恢复 | 重复/乱序、降级状态测试 |
| 搜索同步与恢复 | Canal worker、checkpoint、校验、重建、别名切换 | MySQL 降级边界测试 |
| 分布式锁 Starter | `spring.factories`、属性、条件 Bean、AOP、Lua | `RedisLockExecutorTest` |
| 网关两层限流 | `RateLimitFilter` 固定窗口 Lua、中文 429 | Gateway 编译与脚本演练 |
| 可观测性 | Micrometer Filter、Actuator、`OpsDashboardService` | 面板端到端测试 |

纯原理内容（如 Java SPI 发现机制）没有伪装成业务实现；Starter 使用 Spring Boot 2.4.2 的 `spring.factories`，Java SPI 只在文档中用于解释边界。历史故障与容量数字明确标为推演或压测口径。
