# OA 请示审批测试

```bash
node --test Web/src/views/bpm/oa/work-request/workRequestContract.test.mjs
bash ./podman/build-in-ubuntu.sh ./podman/config/build-server-ubuntu-26.04.yaml
```

Ubuntu 26.04 Server 构建已通过；流程运行验收必须先发布 `oa_work_request` BPM 模型，再验证提交、待办、通过/驳回和状态回写。
