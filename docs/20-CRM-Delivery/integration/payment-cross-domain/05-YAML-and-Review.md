# 显式 YAML 草案与评审门禁

## 配置草案

以下字段是后续实现契约，当前运行程序尚不识别，默认必须保持关闭。命令行仍只能指定 YAML 路径。

```yaml
mitedtsm:
  integration:
    payment:
      enabled: false
      mode: disabled # disabled | record-only | managed
      crm-receivable:
        enabled: false
        pay-app-ref: crm-receivable
        currency: CNY
        require-approved: true
      crm-refund:
        enabled: false
        pay-app-ref: crm-receivable
        require-approved: true
        business-reversal-pay-refund: false
      oa-loan:
        collection-enabled: false
        transfer-enabled: false
      oa-reimbursement:
        transfer-enabled: false
      marketing:
        enabled: false
      internal-callback:
        allowed-clock-skew-seconds: 300
        nonce-retention-seconds: 7200
        signing-key-secret-ref: pay-crm-callback-key
      reconciliation:
        enabled: false
        lookback-minutes: 1440
        batch-size: 200
```

示例配置只能保留逻辑引用和非秘密默认值。真实密钥通过本地未跟踪 secret 或容器 secret 注入；`enabled`
不得因为配置段存在而隐式变为 true。`managed` 才允许程序维护映射，`record-only` 只记录拟执行事实。

## 实现评审清单

- [ ] CRM/BPM/营销/Pay 的聚合所有者和禁止反向依赖没有被破坏；
- [ ] 业务主表未散落 Provider 协议字段，使用独立结算映射；
- [ ] 商户订单/退款编号稳定且具租户、Pay App 唯一约束；
- [ ] 金额采用最小单位整数并有币种精度目录，转换可证明无损；
- [ ] 支付成功不绕过 BPM，审批通过不伪造渠道成功；
- [ ] Provider 签名由 PayClient 校验，匿名回调不信任租户 Header；
- [ ] 跨进程回调有签名、时间窗、nonce/Event ID 去重和网络策略；
- [ ] 重复、乱序、超时、部分退款、失败、未知状态和死信均有测试；
- [ ] 对象权限、支付执行权限和 Pay 配置权限分离；
- [ ] 跨租户、跨 Pay App、金额冲突均失败关闭并报警；
- [ ] 回调/审计日志已脱敏，秘密和原始 Provider 报文不复制到 CRM；
- [ ] YAML 默认关闭、字段校验、secret 引用和三阶段部署说明已完成；
- [ ] 对账任务、人工补偿、双人复核和回滚路径有操作手册；
- [ ] 开启前已通过沙箱 Provider、故障注入、重放和数据清理验收。

## 上线顺序

先只实现关联表与 `record-only` 对账，再实现在线回款，之后才实现审批通过退款；OA 转账和营销付费必须
分别立项。每阶段只能为单一租户/Pay App 灰度，验收完成后再扩大，不允许一次开全域开关。

