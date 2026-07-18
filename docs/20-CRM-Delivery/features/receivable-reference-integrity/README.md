# CRM-FEATURE-051：回款历史引用完整性治理

## 完成内容

- 回款增加四级引用状态：完整、客户缺失、合同缺失/错配、客户与合同双异常；
- 分页支持按引用状态筛选，列表和详情保留不可变的原始客户、合同 ID；
- 对异常引用禁用编辑和错误详情跳转，避免用户把历史异常误认为可维护业务对象；
- 客户统计只纳入有效客户—合同关系，员工业绩继续保留全部审批通过财务台账；
- 真实验收对 API、MySQL 和历史行前后快照做 1:1 对账，不删除、不恢复、不重绑历史记录；
- 运行脚本只接受显式 KDL 配置文件路径。

## 边界

本功能治理历史台账的可见性、查询安全和统计归属，不猜测缺失对象的新归属。若业务负责人后续提供原始合同
证据，需要通过独立的数据修复审批流程处理，不能由运行验收脚本直接改写。

## 验证入口

- 运行验收：`podman/tests/acceptance/verify-crm-receivable-reference-integrity.sh`；
- 无凭据示例：`podman/config/verify-crm-receivable-reference-integrity.example.kdl`；
- 测试与覆盖率：`docs/20-CRM-Delivery/testing/receivable-reference-integrity/`。
