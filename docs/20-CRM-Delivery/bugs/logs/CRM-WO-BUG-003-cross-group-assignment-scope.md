# CRM-WO-BUG-003：普通派单权限可越过处理组边界

- 发现/关闭日期：2026-07-16
- 级别：P1 / 权限与对象范围
- 分支：`develop`

## 现象与根因

处理组候选人校验只验证“目标用户是否属于目标组”，没有验证操作者是否负责目标组。拥有普通
`crm:work-order:assign` 权限的用户可以提交任意组 ID，并把工单指派给该组成员；创建人还可以
改派已经进入处理组的工单。前端禁用或隐藏不能阻止直接 API 请求。

## 修复

服务层把普通派单限制为“无组工单创建人”或“当前处理组负责人”；指定目标组时再次校验操作者
是否为目标组负责人。跨组和全组候选仅允许 `crm:work-order:assign-all`。新增跨组拒绝自动化。

## 验证

`CrmWorkOrderServiceImplTest.groupManagerCannotAssignIntoAnotherGroupWithoutAssignAll` 通过，且拒绝发生在
数据库更新前。
