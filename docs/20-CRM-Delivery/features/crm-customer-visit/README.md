# CRM 客户拜访申请与结果闭环

## 业务闭环

- 申请人从当前有写权限的 CRM 客户中选择拜访对象；联系人必须属于该客户。
- 申请记录计划起止时间、地点、目的、参与人员和附件，并启动 `crm_customer_visit_audit` 审批。
- 审批结果由 BPM 事件回写，列表可直接查看审批进度。
- 只有审批通过且尚未回填的拜访允许登记实际起止时间、结果、结果附件和下次联系时间。
- 结果登记与 CRM 客户跟进记录在同一事务中完成，跟进方式固定使用字典中的“上门拜访”。
- 生成的跟进记录同步更新客户最后跟进内容、下次联系时间和所选联系人下次联系时间。
- 拜访结果只能成功登记一次，避免重复生成客户跟进和统计重复计数。
- 结果登记先锁定拜访行；并发重复提交会串行检查状态，不会生成两条跟进记录。

## 数据与权限

- 业务表：`crm_customer_visit`。
- 申请创建使用 CRM 客户 `WRITE` 对象权限，不能对无权客户发起拜访。
- 查询和详情按申请人隔离；结果回填再次通过客户跟进服务检查当前对象权限。
- 联系人—客户关系、实际时间、审批状态和重复结果均由服务端校验。

## 流程配置

仓库保留无凭据示例：

```bash
cp podman/config/bpm-provision-customer-visit.example.kdl \
  podman/config/bpm-provision-customer-visit-local.kdl
bash podman/operations/bpm/provision-bpm-model.sh podman/config/bpm-provision-customer-visit-local.kdl
```

真实账号只写入 ignored local KDL。全模型清单同时包含客户拜访，空数据卷重建后由 `deploy.sh`
在运行 KDL 的 `startup_mode: replace` 下恢复流程定义。

## 测试

```bash
bash podman/compile.sh podman/config/verify-crm-customer-visit-ubuntu-26.04.kdl
```

- CRM 全量测试：490/490，失败 0、错误 0、跳过 0。
- 客户拜访服务专项：4/4。
- Web 客户拜访契约：3/3，行/分支/函数覆盖率 100%。
- Ubuntu 26.04 production Web 构建通过。
- 真实 MySQL/API 验收：拜访 `1` 审批通过、结果完成，并关联跟进记录 `34`。

专项覆盖率只描述客户拜访 Web 契约，不冒充整个管理端覆盖率。
