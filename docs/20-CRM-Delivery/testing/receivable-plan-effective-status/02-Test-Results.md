# 测试结果

日期：2026-07-14

- `CrmReceivablePlanServiceImplTest`：6/6；
- `CrmReceivablePlanMapperTest`：2/2；
- `CrmReceivablePlanControllerTest`：1/1；
- CRM 全量：149/149；
- Failures：0；Errors：0；Skipped：0；
- Maven reactor：20/20 SUCCESS；
- 目标文件 Prettier、ESLint：通过；
- Ubuntu 26.04 Web production build：通过。

真实 MySQL 双并发计划创建、双回款绑定仍待运行补证，不把单元级锁顺序验证写成压力测试。
