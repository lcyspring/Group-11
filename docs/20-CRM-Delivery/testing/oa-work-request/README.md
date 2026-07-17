# OA 请示审批测试

```bash
node --test Web/src/views/bpm/oa/work-request/workRequestContract.test.mjs
bash ./podman/build-in-ubuntu.sh ./podman/config/build-server-ubuntu-26.04.yaml
```

Ubuntu 26.04 Server 构建已通过；2026-07-17 已通过显式 provision 配置创建并部署 `oa_work_request` 模型及审批角色。后续业务回归继续验证提交、待办、通过/驳回和状态回写。
