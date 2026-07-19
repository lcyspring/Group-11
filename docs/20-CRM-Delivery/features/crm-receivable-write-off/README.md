# 回款核销

回款审批通过只代表业务确认，不代表资金已完成会计核销。本功能新增独立核销台账，支持一笔回款分次
核销；所有有效核销金额之和不得超过回款金额。每条记录可记录人工、银行流水或导入来源及外部流水号。
错误登记使用“冲销”保留审计轨迹，不提供物理删除。

接口：`POST /admin-api/crm/receivable/write-off/create`、`GET /admin-api/crm/receivable/write-off/list`、
`PUT /admin-api/crm/receivable/write-off/reverse`。核销操作需要 `crm:receivable:write-off` 权限。
