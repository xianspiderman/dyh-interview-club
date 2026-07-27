# 发布副本验证记录

验证日期：2026-07-27

## 已实际验证

- Maven 3.9.11 + Oracle JDK 25.0.1：根目录 33 个 reactor 项目执行 `mvn package`，结果 `BUILD SUCCESS`。
- Maven 测试阶段已执行，但项目没有 `*Test.java`/`*Tests.java` 和 Surefire 报告，因此实际自动化测试数量为 0。
- MySQL 8 临时容器：`schema.sql` 与 `demo-data.sql` 各连续执行两次成功。
- 数据库结果：25 张表；`demo-user` 保持 1 条；题目演示数据可查询。
- Redis 7 临时容器：连接成功并返回 `PONG`。
- Auth：应用启动成功；演示用户查询成功；Redis 临时验证码登录成功；收到 Sa-Token；登录用户成功写入数据库；带 Token 查询用户成功。
- Practice：应用启动成功；`GET /practice/set/getSpecialPracticeContent` 返回业务码 200。
- Circle：应用启动成功；`GET /circle/share/circle/list` 返回业务码 200。
- Interview：应用启动成功；`POST /interview/getHistory` 返回业务码 200。
- 静态扫描：原项目标识、待办标记、作者标签、公开 IP、常见云密钥格式、私钥头、邮箱和手机号已检查。

## 仅编译或静态验证

- Subject 全部模块编译成功，Controller 和配置完成静态检查。
- Gateway、OSS、WX 均编译和打包成功。
- Gateway 路由已按 Controller 实际前缀校正：Auth/OSS 移除网关前缀，Subject/Practice/Circle/Interview 保留业务前缀。
- SQL 确认不含 `DROP`，DDL 使用 `IF NOT EXISTS`，演示数据使用 `INSERT IGNORE`。

## 尚未完成的端到端验证

- Subject 启动被本地缺失 RocketMQ 阻塞；错误为无法连接 `localhost:9876`。
- Gateway + Nacos 的完整服务发现和统一鉴权链路。
- Elasticsearch 搜索、RocketMQ 消息、XXL-JOB 调度。
- MinIO 上传、下载和桶管理。
- 微信平台真实回调验签与消息交互。
- 阿里百炼远程接口调用。
- UI/前端流程和演示截图。

这些项目需要对应中间件或外部账号权限；未执行的项目没有标记为成功。
