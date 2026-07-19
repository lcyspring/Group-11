# CRM-CORE-BUG-020：关联合同创建未校验来源商机对象权限

更新日期：2026-07-14

## 级别与关联

- 级别：P0 / 安全；
- 关联：`GAP-IAM-003`、`GAP-OPP-003`、ADR-004/005；
- 状态：已关闭。

## 现象

旧 `POST /crm/contract/create` 只校验 `crm:contract:create` 功能权限。请求可以携带任意
`businessId`，服务端只验证商机存在，不验证调用者对该商机是否拥有对象权限。知道商机
ID 的用户可能将无权操作的商机关联到新合同，并通过请求指定另一客户或负责人。

## 根因

通用合同创建和商机转换共用一个入口，`validateBusiness` 只做存在性检查；方法缺少
`@CrmPermission`，也没有把商机客户、负责人作为服务端不可变来源。

## 修复关键

- 通用入口拒绝携带 `businessId`；
- 新增 `create-from-business` 专用入口；
- 专用 Service 方法要求来源商机 WRITE 对象权限；
- 客户和负责人强制从商机覆盖；
- 前端有商机来源时只调用专用入口。

## 验证

权限注解契约、通用入口拒绝、继承关系和幂等路径均由合同专项 6/6 覆盖；CRM 111/111，
Ubuntu Web production build 通过。
