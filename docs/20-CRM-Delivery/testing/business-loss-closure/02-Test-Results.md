# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.kdl
```

- `CrmBusinessUpdateStatusReqVOTest`：6/6；
- `CrmBusinessServiceImplTest`：3/3；
- CRM 全量：76/76；
- Failures 0、Errors 0、Skipped 0；
- Maven reactor：20/20 SUCCESS。

验证覆盖了原因必填、10/500 字边界、状态互斥、原因 trim、赢单清空原因、保留
最后阶段和并发条件更新失败分支。

## Ubuntu 26.04 Web

```bash
cd podman
bash ./compile.sh ./config/build-web-ubuntu-26.04.kdl
```

Vite production build 成功，`Web/dist-prod/index.html` 为 3694 bytes。

## 未执行项

本轮并发冲突使用 Mapper 返回 0 的 Service 测试覆盖，未对真实 MySQL 发起双请求
竞争；真实锁等待与 API 响应仍应在后续 Podman 运行时场景补证。
