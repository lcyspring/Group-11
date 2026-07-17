# OA 出差申请与审批

## 功能说明

- 员工填写精确到小时的开始、结束时间，目的地、出差原因、预计费用、同行人员和附件。
- 开始时间必须晚于当前时间，结束时间必须晚于开始时间；前后端使用相同的按小时换算天数规则。
- 创建成功后启动 `oa_trip` BPM 流程，审批通过、驳回或取消结果回写出差业务表。
- 列表只显示当前用户自己的申请；详情接口同样校验申请人身份。
- 报销单只能关联当前用户本人、已经审批通过并且已经结束的出差。
- 报销费用日期必须覆盖所关联出差的完整起止日期，服务端再次校验，不能依靠修改请求绕过。

## 运行配置

流程模型使用显式 YAML 创建：

```bash
cp podman/config/bpm-provision-trip.example.yaml podman/config/bpm-provision-trip-local.yaml
# 仅在被 Git 忽略的 local YAML 中填写本机账号
cd podman
bash ./provision-bpm-model.sh ./config/bpm-provision-trip-local.yaml
```

编译和测试使用 `ghcr.io/elel-code/group-11-build-ubuntu:26.04`，配置文件为
`podman/config/verify-oa-trip-ubuntu-26.04.yaml`。
