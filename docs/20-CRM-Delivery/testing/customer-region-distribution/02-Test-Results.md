# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-ubuntu-26.04.yaml
```

- `CrmStatisticsPortraitServiceImplTest`：4/4；
- 区域专项样本：15 个客户、7 个成交客户在城市/省份/国家层均守恒；
- 城市 `110100` 成功展开并包含 `110101/110102`，且不包含上海区域；
- 省份层级搭配城市 ID 时返回空页且 Mapper 调用 0 次；
- CRM 全量：85/85；
- Failures 0、Errors 0、Skipped 0；
- Maven reactor：20/20 SUCCESS。

早期批次曾因测试 import 被误置到类末尾导致 `testCompile` 失败；本批相同补丁定位问题
在执行前源码检查中被发现并修正，详见 `TEST-BUG-006`。

## Ubuntu 26.04 Web

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/build-web-ubuntu-26.04.yaml
```

- Ubuntu 26.04；
- Node 22.22.1、pnpm 11.3.0；
- Vite production build 成功。

## 未执行项

未使用真实 MySQL 构造跨国家客户数据进行 API/页面对账，也未建立两个真实角色进行
组织权限负向测试；自动化已覆盖层级展开和错误层级拒绝，但不能替代运行时权限证据。
