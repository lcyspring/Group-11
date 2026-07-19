# 合同详情初始化可靠性测试计划

日期：2026-07-16。分支：`develop`。

## 目标

验证合同数据未加载时不挂载依赖编号的页签，生命周期组件不发送空编号请求，路由仅接受正安全整数合同编号。

## 门禁

- `contractDetailGuard.test.mjs`；
- 合同详情与生命周期 ESLint；
- Ubuntu 26.04 Web production build。
