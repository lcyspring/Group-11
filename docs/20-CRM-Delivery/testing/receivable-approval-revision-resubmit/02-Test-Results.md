# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

- `CrmReceivableApprovalServiceImplTest`：11/11；
- `CrmReceivablePlanServiceImplTest`：3/3；
- `CrmReceivableAmountValidationTest`：3/3；
- `CrmReceivableMapperTest`：1/1；
- `CrmReceivableErrorCodeTest`：1/1；
- 既有提交额度回归：1/1；
- 本批回款专项：20/20；
- CRM 全量：143/143；
- Failures：0；Errors：0；Skipped：0；
- Maven reactor：20/20 SUCCESS。

## Ubuntu 26.04 Web

- 目标文件 Prettier：通过；
- 目标文件 ESLint：通过；
- CRM 统计纯函数：7/7；
- Vite production build：成功。

## 待补运行证据

本批完成代码和单元级锁顺序/条件更新验证，但未在已发布的真实
`crm-receivable-audit` 上执行双 HTTP 提交及双终态回调压力测试，不把它记录为已完成。
