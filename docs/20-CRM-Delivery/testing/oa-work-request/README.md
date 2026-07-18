# OA 请示审批测试

```bash
node --test Web/src/views/bpm/oa/work-request/workRequestContract.test.mjs
bash ./podman/compile.sh ./podman/config/build-server-ubuntu-26.04.kdl
```

Ubuntu 26.04 Server 构建已通过；2026-07-17 已通过显式 provision 配置创建并部署 `oa_work_request` 模型及审批角色。运行态已真实创建请示与流程实例，随后取消流程并清理测试业务数据，状态监听正确回写为取消。
