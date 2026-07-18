# 测试结果

日期：2026-07-16。构建环境：Ubuntu 26.04 容器、rootless Podman。

| 检查 | 结果 |
|---|---|
| CRM 自动化 | 369/369，通过，失败 0、错误 0、跳过 0 |
| ERP 履约专项 | 4/4，通过 |
| Web 履约状态纯函数 | 3/3，通过 |
| Web 专项 ESLint | 零警告 |
| Web production build | 通过，生成 `Web/dist-prod/index.html` |
| SQL 迁移重复执行 | 连续两次通过；3 张 CRM 表和 2 个 ERP 唯一索引存在 |
| 配置属性校验 | 完整策略通过；缺币种模式、舍入模式、容差及越界精度/错误长度被拒绝 |
| 真实 API 验收 | 通过：合同 28 → ERP 订单 16；重复创建返回同一订单；ERP 审核与数量刷新成功；清理残留 0 |

执行入口：

```bash
cd podman
bash ./compile.sh ./config/test-crm-erp-fulfillment-ubuntu-26.04.kdl
bash ./tests/acceptance/verify-crm-erp-fulfillment.sh ./config/verify-crm-erp-fulfillment-local.kdl
```
