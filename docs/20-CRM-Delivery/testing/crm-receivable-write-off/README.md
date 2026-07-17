# 回款核销测试

```bash
bash ./podman/build-in-ubuntu.sh ./podman/config/test-crm-ubuntu-26.04.yaml
bash ./podman/build-in-ubuntu.sh ./podman/config/build-web-ubuntu-26.04.yaml
```

专项测试覆盖：仅审批通过可核销、分次金额守恒、外部流水号幂等、冲销保留记录、数据权限和前端操作入口。
CRM 全量门禁应保持全绿；前端回款核销契约测试位于 `Web/src/views/crm/receivable/receivableWriteOff.test.mjs`。
