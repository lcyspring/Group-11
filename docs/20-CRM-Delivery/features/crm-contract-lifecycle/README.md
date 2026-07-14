# CRM-FEATURE-028：合同签署、附件与不可变版本轨迹

完成日期：2026-07-14。依据：GAP-CTR-001、ADR-002/004/006/015。

## 交付结论

CRM 合同现已区分审批通过、实际签署和生效事实。实现没有向 `crm_contract` 主表继续堆叠状态，
而是增加附件、签署事实和不可变变更轨迹三个独立聚合，并在合同详情提供“签署与版本”页签。

## 模型与不变量

- `crm_contract_attachment` 保存文件元数据、业务类别、所属版本和锁定状态；
- `crm_contract_signing` 每租户每合同最多一条实际签署事实，保存方式、时间、经办人、签署副本和 Provider 幂等号；
- `crm_contract_change_record` 按合同保存唯一递增序号、业务版本、动作、原因、合同快照和产品快照；
- 只有审批通过合同可以签署；签署附件必须属于同一合同且类别为“签署副本”；
- 正式签署后副本锁定，普通删除接口不能移除；合同级行锁串行化附件、签署和轨迹写入；
- 创建、修改、提交、审批终态、签署和签署作废进入同一轨迹；摘要接口不返回完整 JSON 快照；
- 相同签署命令返回原签署 ID；相同原因的作废重试直接成功，不重复调用 Provider 或写轨迹。

## Provider 与显式配置

签署边界由 `CrmContractSignProvider` 隔离。Provider 必须声明支持的签署方式，后端强校验，前端
根据接口能力集合生成选项。当前显式配置为：

```yaml
mitedtsm:
  crm:
    contract-sign:
      provider: local-record
```

`local-record` 只支持线下签署事实登记，不接受电子签命令，也不伪造外部签章编号。命令行仍只
接收 YAML 文件路径。

## 接口与前端

- `POST /crm/contract-lifecycle/attachment`：登记规范化附件；
- `DELETE /crm/contract-lifecycle/attachment`：删除未锁定附件；
- `PUT /crm/contract-lifecycle/sign`：执行幂等签署命令；
- `PUT /crm/contract-lifecycle/sign-void`：执行幂等签署作废；
- `GET /crm/contract-lifecycle/get`：返回签署、附件、轨迹摘要和 Provider 能力。

权限拆分为附件维护、签署、签署作废和既有合同查询，对象范围继续复用 CRM 合同权限。

## 未宣称范围

- 未接入任何电子签供应商、回调验签、签章位置或证书；
- 未实现正式补充协议/新版本的独立审批命令；现有版本轨迹覆盖草稿修订、审批、签署和作废；
- 未开放完整历史快照读取接口；快照保存在数据库，未来只能通过独立权限接口按需读取。
