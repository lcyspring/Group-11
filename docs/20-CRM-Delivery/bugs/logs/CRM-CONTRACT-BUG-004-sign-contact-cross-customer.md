# CRM-CONTRACT-BUG-004：合同可绑定其他客户的签约联系人

发现日期：2026-07-14。状态：Fixed。

## 现象与风险

合同保存时只验证 `signContactId` 对应联系人存在，没有验证联系人所属客户与合同客户一致。
因此可构造“客户 A 的合同 + 客户 B 的联系人”，详情页会展示错误签约主体，并可能污染审批、
签署记录和后续合同附件权限。

## 根因

`CrmContractServiceImpl.validateRelationDataExists` 调用的 `validateContact` 只做主键存在性校验，
没有执行跨聚合关系不变量。

## 修复

- 读取签约联系人并校验 `contact.customerId == contract.customerId`；
- 联系人不存在继续返回统一的 `CONTACT_NOT_EXISTS`；
- 跨客户时返回独立业务码 `1020000012`；
- 创建和修改合同共用同一校验入口，商机转换合同也不能绕过。

## 测试

新增 `createContractRejectsSignContactFromAnotherCustomer`，验证跨客户联系人被拒绝且合同 Mapper
不会写入。后续签署与附件模型必须继续以合同对象权限为根，不能依赖前端下拉过滤作为安全边界。
