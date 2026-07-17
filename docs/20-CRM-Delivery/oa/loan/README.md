# OA 借款申请、审批与还款

## 功能说明

- 员工可按差旅、采购或其他用途发起借款，并可显式关联本人出差申请。
- 借款额度由服务端 YAML 配置和用户岗位编码决定，不在前端硬编码；当前支持员工、经理、总监三级。
- 超出岗位额度不会伪装成普通申请，业务记录和流程变量都会标记 `escalatedApproval`，供审批模型升级处理。
- 存在未还清借款时阻止再次申请，避免绕过资金占用约束。
- 审批通过后生成未还余额；驳回、取消不会生成待还款金额。
- 支持分次还款和一次性还清，记录还款时间、交易流水号和备注。
- 服务端拒绝超额还款、未来还款时间、非本人借款和未审批通过借款的还款请求。
- 列表、详情、审批进度和还款记录均按当前登录用户隔离。

## 显式配置

岗位额度位于 Server YAML：

```yaml
mitedtsm:
  bpm:
    oa:
      loan:
        employee-limit: 5000
        manager-limit: 20000
        director-limit: 50000
        manager-post-codes: [manager]
        director-post-codes: [director]
```

流程模型使用无凭据示例复制为 ignored 本机配置：

```bash
cp podman/config/bpm-provision-loan.example.yaml podman/config/bpm-provision-loan-local.yaml
cd podman
bash ./provision-bpm-model.sh ./config/bpm-provision-loan-local.yaml
```

模型 key 固定为 `oa_loan`。全新数据卷由 `up.sh full` 通过
`bpm-provision-all-local.yaml` 自动恢复，不允许依靠数据库中偶然残留的 Flowable 定义。

## 测试与覆盖率

统一使用公共 Ubuntu 26.04 工具链：

```bash
bash podman/build-in-ubuntu.sh podman/config/verify-oa-loan-ubuntu-26.04.yaml
```

当前证据：

- BPM 测试：67 项执行，0 失败、0 错误，6 项原有条件跳过；借款服务专项 4/4 通过。
- Web 借款契约：3/3 通过。
- Web 借款专项：行、分支、函数覆盖率均为 100%。
- Web production 构建成功，产物写入 `Web/dist-prod/`。

覆盖率是借款契约专项覆盖率，不冒充整个 Web 或整个 BPM 模块覆盖率。

## 用户验收

1. 普通员工申请额度内借款，确认页面显示岗位额度且不会标记升级审批。
2. 申请超额借款，确认列表与详情显示升级审批，流程仍可正常提交。
3. 审批通过后确认未还金额等于借款金额。
4. 登记部分还款，确认未还金额立即减少且详情出现还款记录。
5. 登记剩余金额，确认借款变为已还清且不再显示还款操作。
6. 尝试超额还款、未来时间还款和存在未还借款时新建申请，确认服务端拒绝。
