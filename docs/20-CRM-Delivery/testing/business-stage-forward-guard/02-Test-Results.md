# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.yaml
```

- `CrmBusinessUpdateStatusReqVOTest`：7/7；
- `CrmBusinessServiceImplTest`：5/5；
- CRM 全量：105/105；
- Failures：0；Errors：0；Skipped：0；
- Maven reactor：20/20 SUCCESS。

新增用例覆盖空白推进说明、前向推进成功和阶段回退拒绝；既有输单/无效原因、赢单、
并发冲突和最后阶段保留用例全部回归通过。

## Ubuntu 26.04 Web

- Prettier：通过；
- ESLint：通过；
- CRM 统计纯函数：7/7；
- Vite production build：成功；
- 全部通过 `localhost/mitedtsm-build-ubuntu:26.04` builder 执行，未使用宿主机
  JDK、Node 或 pnpm。

## Bug 回归

`CRM-CORE-BUG-019` 修复前前后端都允许选择/提交较低排序阶段；修复后前端不展示回退
候选，后端对绕过页面的请求返回 `1020002005`。
