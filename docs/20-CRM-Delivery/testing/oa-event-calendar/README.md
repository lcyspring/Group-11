# OA 日程日/周/月视图验证

## 自动化

执行：

```bash
node --test Web/src/views/bpm/oa/event/eventCalendarView.test.mjs
```

覆盖日、周、月查询范围，上一/下一周期、返回今天，以及编辑时间与视图范围隔离。

## Ubuntu 26.04 构建门禁

2026-07-17 使用以下命令通过生产构建：

```bash
bash ./podman/compile.sh ./podman/config/build-web-ubuntu-26.04.yaml
```

工具链专项覆盖率：行 96.45%、分支 90.63%、函数 100%；Vite 生产构建成功。
