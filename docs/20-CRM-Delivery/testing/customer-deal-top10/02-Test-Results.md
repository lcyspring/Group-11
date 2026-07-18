# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.yaml
```

- `CrmStatisticsCustomerServiceImplTest`：7/7；
- `CrmStatisticsCustomerMapperTest`：2/2；
- CRM 全量：82/82；
- Failures 0、Errors 0、Skipped 0；
- Maven reactor：20/20 SUCCESS。

## Ubuntu 26.04 Web

```bash
cd podman
bash ./compile.sh ./config/build-web-ubuntu-26.04.yaml
```

- Ubuntu 26.04；
- Node 22.22.1、pnpm 11.3.0；
- Vite production build 成功。

## 未执行项

尚未准备真实 MySQL 的多客户同额、跨负责人转移和超过 10 名数据集进行接口对账；
当前 Mapper 测试属于 SQL 契约测试，不能代替真实数据库排序与组织权限负向证据。
