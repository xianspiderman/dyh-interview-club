# ApiPost 接口测试说明

本文档依据发布副本中的实际 Controller 整理。`ANY` 表示源码使用 `@RequestMapping` 且未限制 HTTP 方法；在 ApiPost 中，有 JSON 请求体的接口建议使用 POST。

## 环境变量

在 ApiPost 环境中配置：

| 变量 | 示例 |
| --- | --- |
| `authBaseUrl` | `http://localhost:3011` |
| `subjectBaseUrl` | `http://localhost:3010` |
| `practiceBaseUrl` | `http://localhost:3013` |
| `circleBaseUrl` | `http://localhost:3014` |
| `interviewBaseUrl` | `http://localhost:3015` |
| `ossBaseUrl` | `http://localhost:4000` |
| `wxBaseUrl` | `http://localhost:3012` |
| `gatewayBaseUrl` | `http://localhost:5000` |

JSON 接口统一添加 `Content-Type: application/json`。业务服务从请求头读取 `loginId`；经网关调用时，正常情况下由认证链路传递。Sa-Token 请求头名为 `satoken`，配置了前缀时值形如 `Bearer <token>`。

## Auth

直连前缀：`{{authBaseUrl}}`。经网关调用时在路径前添加 `/auth`，例如 `/auth/user/getUserInfo`。

| 方法 | 路径 | 参数/请求体 |
| --- | --- | --- |
| ANY | `/user/register` | `AuthUserDTO` JSON |
| ANY | `/user/update` | `AuthUserDTO` JSON |
| ANY | `/user/getUserInfo` | `AuthUserDTO` JSON，至少 `userName` |
| ANY | `/user/listByIds` | 用户名字符串数组 |
| ANY | `/user/logOut` | Query：`userName` |
| ANY | `/user/delete` | `AuthUserDTO` JSON |
| ANY | `/user/changeStatus` | `AuthUserDTO` JSON，至少 `status` |
| ANY | `/user/doLogin` | Query：`validCode` |
| ANY | `/permission/add` | `AuthPermissionDTO` JSON |
| ANY | `/permission/update` | `AuthPermissionDTO` JSON |
| ANY | `/permission/delete` | `AuthPermissionDTO` JSON |
| ANY | `/permission/getPermission` | Query：`userName` |
| ANY | `/role/add` | `AuthRoleDTO` JSON |
| ANY | `/role/update` | `AuthRoleDTO` JSON |
| ANY | `/role/delete` | `AuthRoleDTO` JSON |
| ANY | `/rolePermission/add` | `AuthRolePermissionDTO` JSON |

查询演示用户：

```bash
curl -X POST "http://localhost:3011/user/getUserInfo" \
  -H "Content-Type: application/json" \
  -d '{"userName":"demo-user"}'
```

登录接口要求微信服务或其他上游先在 Redis 写入 `loginCode.<validCode>` 对应的用户标识。不要在共享文档中保存返回的真实 Token。

## Subject

直连和网关路径均保留 `/subject` 前缀。

| 方法 | 路径 | 参数/请求体 |
| --- | --- | --- |
| POST | `/subject/category/add` | `SubjectCategoryDTO` |
| POST | `/subject/category/queryPrimaryCategory` | `SubjectCategoryDTO` |
| POST | `/subject/category/queryCategoryByPrimary` | `SubjectCategoryDTO` |
| POST | `/subject/category/update` | `SubjectCategoryDTO` |
| POST | `/subject/category/delete` | `SubjectCategoryDTO` |
| POST | `/subject/category/queryCategoryAndLabel` | `SubjectCategoryDTO` |
| POST | `/subject/add` | `SubjectInfoDTO` |
| POST | `/subject/getSubjectPage` | `SubjectInfoDTO` |
| POST | `/subject/querySubjectInfo` | `SubjectInfoDTO` |
| POST | `/subject/getSubjectPageBySearch` | `SubjectInfoDTO`；依赖 Elasticsearch |
| POST | `/subject/getContributeList` | 无请求体 |
| POST | `/subject/pushMessage` | Query：`id`；依赖 RocketMQ |
| POST | `/subject/label/add` | `SubjectLabelDTO` |
| POST | `/subject/label/update` | `SubjectLabelDTO` |
| POST | `/subject/label/delete` | `SubjectLabelDTO` |
| POST | `/subject/label/queryLabelByCategoryId` | `SubjectLabelDTO` |
| ANY | `/subjectLiked/add` | `SubjectLikedDTO` |
| POST | `/subjectLiked/getSubjectLikedPage` | `SubjectLikedDTO` |
| ANY | `/subjectLiked/update` | `SubjectLikedDTO` |
| ANY | `/subjectLiked/delete` | `SubjectLikedDTO` |

示例：

```bash
curl -X POST "http://localhost:3010/subject/category/queryPrimaryCategory" \
  -H "Content-Type: application/json" \
  -H "loginId: demo-user" \
  -d '{}'
```

## Practice

| 方法 | 路径 | 参数/请求体 |
| --- | --- | --- |
| ANY | `/practice/set/getSpecialPracticeContent` | 无请求体 |
| POST | `/practice/set/addPractice` | `GetPracticeSubjectListReq` |
| POST | `/practice/set/getSubjects` | `GetPracticeSubjectsReq` |
| POST | `/practice/set/getPracticeSubject` | `GetPracticeSubjectReq` |
| POST | `/practice/set/getPreSetContent` | `GetPreSetReq` |
| POST | `/practice/set/getUnCompletePractice` | `GetUnCompletePracticeReq` |
| POST | `/practice/detail/submitSubject` | `SubmitSubjectDetailReq` |
| POST | `/practice/detail/submit` | `SubmitPracticeDetailReq` |
| POST | `/practice/detail/getScoreDetail` | `GetScoreDetailReq` |
| POST | `/practice/detail/getSubjectDetail` | `GetSubjectDetailReq` |
| POST | `/practice/detail/getReport` | `GetReportReq` |
| POST | `/practice/detail/getPracticeRankList` | 无请求体 |
| POST | `/practice/detail/giveUp` | Query：`practiceId` |

```bash
curl "http://localhost:3013/practice/set/getSpecialPracticeContent"
```

## Circle

| 方法 | 路径 | 参数/请求体 |
| --- | --- | --- |
| GET | `/circle/sensitive/words/save` | Query：`words`、`type` |
| GET | `/circle/sensitive/words/remove` | Query：`id` |
| GET | `/circle/share/circle/list` | 无请求体 |
| POST | `/circle/share/circle/save` | `SaveShareCircleReq` |
| POST | `/circle/share/circle/update` | `UpdateShareCircleReq` |
| POST | `/circle/share/circle/remove` | `RemoveShareCircleReq` |
| POST | `/circle/share/comment/save` | `SaveShareCommentReplyReq` |
| POST | `/circle/share/comment/remove` | `RemoveShareCommentReq` |
| POST | `/circle/share/comment/list` | `GetShareCommentReq` |
| GET | `/circle/share/message/unRead` | 无请求体 |
| POST | `/circle/share/message/getMessages` | `GetShareMessageReq` |
| POST | `/circle/share/moment/save` | `SaveMomentCircleReq` |
| POST | `/circle/share/moment/getMoments` | `GetShareMomentReq` |
| POST | `/circle/share/moment/remove` | `RemoveShareMomentReq` |

```bash
curl "http://localhost:3014/circle/share/circle/list"
```

敏感词保存和删除在现有代码中使用 GET，测试时避免指向生产环境。

## Interview

| 方法 | 路径 | 参数/请求体 |
| --- | --- | --- |
| POST | `/interview/analyse` | `InterviewReq`；远程简历/职位 URL 分析 |
| POST | `/interview/start` | `StartReq` |
| POST | `/interview/submit` | `InterviewSubmitReq` |
| POST | `/interview/getHistory` | `InterviewHistoryReq` |
| GET | `/interview/detail` | Query：`id` |

查询空的演示用户面试历史：

```bash
curl -X POST "http://localhost:3015/interview/getHistory" \
  -H "Content-Type: application/json" \
  -H "loginId: demo-user" \
  -d '{"pageInfo":{}}'
```

`ALI_BL` 引擎调用需要 `ALIYUN_BAILIAN_API_KEY`。不要把 API Key 写入 ApiPost 公共环境或导出文件。

## OSS

| 方法 | 路径 | 参数 |
| --- | --- | --- |
| ANY | `/testGetAllBuckets` | 无 |
| ANY | `/getUrl` | Query：`bucketName`、`objectName` |
| ANY | `/upload` | multipart：`uploadFile`、`bucket`、`objectName` |

经网关调用时使用 `/oss/upload` 等路径，Gateway 会移除 `/oss` 前缀。

```bash
curl -X POST "http://localhost:4000/upload" \
  -F "uploadFile=@./demo.png" \
  -F "bucket=demo" \
  -F "objectName=demo.png"
```

## WX

| 方法 | 路径 | 参数 |
| --- | --- | --- |
| GET | `/callback` | Query：`signature`、`timestamp`、`nonce`、`echostr` |
| POST | `/callback` | XML 请求体；Query 同上，可选 `msg_signature` |

验签依赖 `WECHAT_CALLBACK_TOKEN`，只能使用微信平台产生的真实签名完成端到端验证。
