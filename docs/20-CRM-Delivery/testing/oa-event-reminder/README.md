# OA 日程提醒验证

## 编译门禁

```bash
bash ./podman/compile.sh ./podman/config/build-server-ubuntu-26.04.kdl
```

2026-07-17 在 Ubuntu 26.04 工具链中通过，BPM 与 CRM 模块编译成功，最终 Spring Boot JAR 构建成功。

## 状态机验收

- `0` 待发送：满足提前时间窗口后可被扫描；
- `1` 发送中：条件更新抢占，其他执行者跳过；
- `2` 已发送：记录发送时间，不再重复发送；
- 异常：回到 `0` 并记录错误，下一轮重试。
