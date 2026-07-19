# 独立 OA 任务工作台测试

```bash
bash ./podman/compile.sh ./podman/config/verify-oa-event-ubuntu-26.04.kdl
```

Ubuntu 26.04 Server 构建门禁覆盖后端编译；Web 门禁覆盖前端接口、状态操作契约和截止时间响应标准化。时间测试执行数值时间戳、数字字符串、编辑回填和非法输入场景。提醒状态 `0/1/2` 的抢占、发送和失败释放由 BPM 服务编译门禁覆盖。

2026-07-19 门禁结果：任务工作台 2/2、日程 7/7、请示 2/2、公共保存提示 2/2，合计 Web 13/13；BPM 66/66；ESLint 0 warning；Vite 8 生产构建和三语言包通过。日期时间可执行模块行、分支、函数覆盖率均为 100%，报告位于 `coverage/verify-oa-event.lcov`。
