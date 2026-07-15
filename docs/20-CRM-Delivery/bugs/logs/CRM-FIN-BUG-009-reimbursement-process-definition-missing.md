# CRM-FIN-BUG-009：报销审批流程定义未部署

- 发现日期：2026-07-15
- 分支：`develop`
- 状态：Open

## 现象

报销服务已使用 YAML 键 `crm-reimbursement-audit` 提交 BPM，但本地 Flowable 的 `ACT_RE_PROCDEF` 没有任何流程定义。草稿 CRUD 可运行，提交审批会返回流程定义不存在。

## 原因

项目关闭了 `/resources/processes` 自动部署，审批定义需要由 BPM 模型治理流程显式创建和发布。仅在 Java 中写流程键不能产生可执行流程。

## 修复要求

1. 通过 BPM 模型管理创建并发布 `crm-reimbursement-audit`；
2. 审批人规则必须显式配置，不能在 Java 或 BPMN 中硬编码用户编号；
3. 验证提交、待办、通过、驳回、取消、修订重提和旧回调；
4. 发布信息和运行证据进入报销功能与测试目录。

在上述验收完成前，报销审批集成不得标记完成。
