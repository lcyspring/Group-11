# CRM 弹窗操作多语言测试计划

日期：2026-07-16。分支：`develop`。

## 目标

扫描整个 CRM 前端目录，禁止确认和取消按钮引用不存在的 `dialog` 多语言键，并验证受影响目录可通过 ESLint 与生产构建。

## 门禁

- `crmDialogI18n.test.mjs`；
- 线索、工单、产品、权限和跟进目录 ESLint；
- Ubuntu 26.04 Web production build。
