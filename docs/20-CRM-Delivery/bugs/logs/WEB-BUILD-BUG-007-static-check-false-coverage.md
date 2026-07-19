# WEB-BUILD-BUG-007：静态检查被错误要求生成测试覆盖率

日期：2026-07-19。分支：`develop`。级别：P1/测试门禁。状态：已关闭。

## 现象与根因

迁移 Deno 原生覆盖率后，所有存在 `web.test_script` 的 KDL 都默认执行 `deno coverage`。纯 ESLint、
`ts:check` 和空脚本不会生成 coverage profile，因此成功的静态检查会在收尾阶段被误判失败。

## 修复关键

- 新增显式 KDL 字段 `web.coverage_enabled`，不依据脚本名称猜测；
- 真正执行 Deno Test 的任务设为 `true`，lint、类型检查和空脚本设为 `false`；
- 只有启用时才创建 profile、执行阈值门禁和输出 LCOV；启用但没有测试脚本直接拒绝配置；
- runtime-config 门禁检查所有现行构建 KDL 的布尔值和 0～100 阈值。

## 回归

`lint:crm-activity` 在 Ubuntu 26.04 容器中成功且不进入 LCOV 阶段；`verify:web-media` 为 4/4，行、
分支、函数覆盖率均为 100%，LCOV 成功输出。
