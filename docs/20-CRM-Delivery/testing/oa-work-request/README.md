# OA 请示审批测试

```bash
bash ./podman/compile.sh ./podman/config/verify-oa-event-ubuntu-26.04.kdl
```

Ubuntu 26.04 Server 构建已通过；2026-07-17 已通过显式 provision 配置创建并部署 `oa_work_request` 模型及审批角色。运行态已真实创建请示与流程实例，随后取消流程并清理测试业务数据，状态监听正确回写为取消。

2026-07-19 增加审批状态字典、详情、进度、本人取消、发起/完成时间和缓存刷新门禁。请示 Web 契约 2/2，包含请示完成时间回写的 BPM 全量测试 66/66，ESLint 0 warning，Vite 8 与三语言包完整性通过。
