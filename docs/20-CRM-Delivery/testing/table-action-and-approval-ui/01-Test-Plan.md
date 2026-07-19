# 表格操作与审批前端可靠性测试计划

日期：2026-07-16。分支：`develop`。

## 目标

1. 全量识别管理端表格操作列，三项以上动作不得继续挤在不足宽度的固定列；
2. 通用操作组件支持稳定横排和弹出菜单，按钮不收缩且菜单点击后关闭；
3. CRM 财务四类列表使用统一组件，业务状态与权限条件保持不变；
4. BPM 待办、已办及 CRM BPM 待办在缓存页重新激活时刷新且首次不双请求；
5. 财务新建/编辑弹窗只使用存在的多语言确认和取消键；
6. 审批详情等隐藏路由保存翻译键，页签按当前语言显示，中文审批详情标题为“流程详情”；
7. 在 Ubuntu 26.04 Podman 环境完成专项测试、ESLint 和 production build。

## 门禁

以下分析、SFC 契约、Deno Test 与 ESLint 均由 `verify:crm-finance-approval-ui` 任务在 Ubuntu 26.04
工具链容器内统一执行，不在宿主机逐项调用 Node 或包管理器：

- 表格操作静态分析与 SFC 操作列验证；
- `financeApprovalUi.test.mjs`、`todoRefresh.test.mjs`、`remainingTitle.test.mjs`；
- 相关 CRM、BPM 与通用组件 ESLint，warning 上限为 0。

唯一成员入口：

```bash
bash podman/compile.sh podman/config/verify-table-actions-ubuntu-26.04.kdl
```
