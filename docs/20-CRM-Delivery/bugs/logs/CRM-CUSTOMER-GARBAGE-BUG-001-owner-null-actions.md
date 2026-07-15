# CRM-CUSTOMER-GARBAGE-BUG-001：垃圾客户详情错误显示公海动作

- 日期：2026-07-15
- 分支：`develop`
- 状态：已修复

## 问题

客户详情用 `ownerUserId` 是否为空判断“领取”和“分配”按钮。公海和垃圾池都没有负责人，
因此垃圾客户会被错误识别成公海客户；“放入公海”同样没有校验显式池状态。

## 修复

- 增加管理端 `CustomerPoolStatus` 枚举；
- 领取和分配仅在 `PUBLIC` 展示；
- 放入公海仅在 `OWNED`、存在负责人且当前用户拥有 OWNER 权限时展示；
- 客户池专项 ESLint 纳入详情入口文件，防止后续修改逃逸检查。

## 验证

Ubuntu 26.04 客户池专项 ESLint 通过，0 warning、0 error。
