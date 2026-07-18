# CRM 报销 BPM 模型治理

- 分支：`develop`
- 状态：完成

## 能力

- 命令行只接收 KDL 配置文件路径；
- 通过系统正式 API 管理审批角色、角色成员、流程分类、模型和发布；
- 角色代码、审批账号和模型字段均来自 KDL，不在 Java 或 BPMN 中硬编码用户编号；
- 审批角色所需的 BPM 待办查询、审批、流程详情和报销读取权限由 KDL 列出，脚本按权限码解析菜单及全部祖先菜单后合并授权；
- 本机凭据 KDL 独立忽略，可上传配置仅保留 `CHANGE_ME`；
- 重复执行会比较规范化后的模型内容，未变化时跳过发布；
- 空审批角色时转交模型管理员，避免产生无人任务；
- 发起人可取消运行中的报销流程，审批意见设为必填。

## 使用

复制 `podman/config/bpm-provision.example.kdl` 为本机忽略的配置，填入账号后执行：

```bash
cd podman
bash ./operations/bpm/provision-bpm-model.sh ./config/bpm-provision-local.kdl
```

审批模型归 BPM 治理，报销主单、明细、金额和动作轨迹仍归 CRM 财务域。审批通过只确认 CRM 业务状态，不代表银行付款、报销打款或会计入账。
