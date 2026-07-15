# CRM 财务域报销管理

- 分支：`develop`
- 状态：后端 CRUD 完成，前端与流程部署待完成

本功能补齐财务域第四类业务真源，包含租户费用分类、报销主单、费用明细、BPM 审批和不可变动作轨迹。

## 业务规则

- 编号前缀和 BPM 流程定义键由 YAML 显式配置；
- 报销金额由服务端按明细计算；
- 关联合同后校验合同与客户一致；
- 草稿、驳回、取消可修订，修订后回到草稿；
- 审批中、审批通过禁止直接改写；
- 页面查询继续复用 CRM 对象级数据权限。
- 附件只能经报销专用入口上传，并绑定 `crm-protected/reimbursement/{报销ID}` 受保护目录；外链和跨对象 URL 被拒绝。

运行时只使用显式 YAML：`podman/config/runtime-local-rebuild-server.yaml` 会打包当前 Ubuntu 构建产物、执行幂等迁移并只替换 Server，保留 Web、Mall 和基础设施容器。

## 数据模型

- `crm_expense_category`
- `crm_reimbursement`
- `crm_reimbursement_item`
- `crm_reimbursement_action_record`

费用统计以审批通过的 `crm_reimbursement_item` 为来源，不重复维护可变的汇总金额。

## 当前运行证据

- 实例：`BX202607-0001`；
- 明细：2 条；
- 明细合计与主单总额：`124.000000 CNY`；
- 草稿更新后版本 0 → 1，受保护附件 1 个；
- 两次执行兼容迁移均成功；
- Flowable 当前无已部署定义，`crm-reimbursement-audit` 需要在下一阶段部署后再验收审批任务。
