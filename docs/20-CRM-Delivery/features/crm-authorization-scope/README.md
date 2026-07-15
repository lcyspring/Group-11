# CRM 统一组织授权范围

日期：2026-07-15

分支：`develop`

## 已实现

- 新增 `CrmAuthorizationService`，统一解析平台 ALL、CUSTOM、DEPT、DEPT_AND_CHILD、SELF 数据范围；
- 将部门编号转换为 CRM `owner_user_id` 集合，不要求 CRM 表增加重复的 `dept_id`；
- 对象详情读取合并直接协作权限和组织范围；组织范围严格只读；
- “下属负责”改为下属集合与组织范围交集，消除纯上下级关系越权；
- 新增 `sceneType=4` 的“组织范围”列表，并覆盖九类 CRM 对象；
- 发票列表补齐此前缺失的归属切换；
- 管理员角色代码迁入显式 YAML `mitedtsm.crm.authorization.admin-role-codes`；
- 导出逐对象只认当前用户直接 WRITE/OWNER，不接受部门 READ 或下属 OWNER；
- 中、英、阿三语前端标签同步。

## 边界

本批次不批量改写系统角色数据；默认角色矩阵由管理员在现有角色/菜单能力中配置。租户隔离、状态机、
金额校验和附件父对象授权保持独立门禁。工单的创建人/处理人/query-all 场景是专用语义，不套用通用
CRM sceneType。
