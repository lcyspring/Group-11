# CRM-IAM-BUG-005：对象权限等级使用精确匹配

- 模块：CRM / 对象授权
- 状态：已关闭
- 严重性：P0
- 分支：`develop`

## 现象与根因

真实 API 验收中，线索负责人可以创建任务，却无法开始自己的任务并返回“没有权限”。旧
`hasPermission` 按权限数字精确相等判断，错误地要求 OWNER 必须另有 WRITE，WRITE 也不能
满足 READ，违背系统声明的 `OWNER > WRITE > READ`。

## 修复关键

- 统一复用 `CrmAuthorizationService.isGranted` 等级判定，不再复制数值比较；
- CRM 管理员继承既有授权；OWNER 满足 WRITE/READ，WRITE 满足 READ，READ 不升级为 WRITE；
- 无授权时只为显式公共、未转换、空负责人的线索开放 READ；
- 已转换线索、非公共客户和在管对象继续失败关闭。

## 验证

新增等级继承、反向不提升和公共对象边界 3 项测试，CRM 权限 Service 11/11、CRM 全量
354/354 通过。重新部署后真实任务开始、完成和线索活动迁移验收通过。
