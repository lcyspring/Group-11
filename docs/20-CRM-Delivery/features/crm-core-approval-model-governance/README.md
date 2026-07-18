# CRM 核心审批模型治理

- 分支：`develop`
- 状态：完成

## 已治理模型

| 业务 | 模型键 | 审批角色 |
|---|---|---|
| 合同 | `crm-contract-audit` | `crm_contract_approver` |
| 回款 | `crm-receivable-audit` | `crm_finance_approver` |
| 退款/冲销 | `crm-receivable-refund-audit` | `crm_finance_approver` |
| 报销 | `crm-reimbursement-audit` | `crm_finance_approver` |

每份 YAML 显式描述租户、治理账号、审批角色、审批账号、权限码、流程分类、模型字段、业务表单路径和审批节点名称。脚本通过正式 API 治理角色与模型，不读取项目 Docker 配置，也不使用资源目录自动部署。

审批角色通过权限码获得所需 BPM 和 CRM 对象权限，脚本自动合并祖先菜单与既有授权。模型和权限未变化时均不写入，避免每次启动无意义地产生新流程版本。

仓库提供四份 `bpm-provision*.example.yaml`，真实密码只写入已忽略的 `*-local.yaml`。命令始终只有配置文件路径：

```bash
cd podman
bash ./operations/bpm/provision-bpm-model.sh ./config/bpm-provision-refund-local.yaml
```
