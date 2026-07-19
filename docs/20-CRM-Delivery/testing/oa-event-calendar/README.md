# OA 日程日/周/月视图验证

## 自动化

执行：

```bash
bash ./podman/compile.sh ./podman/config/verify-oa-event-ubuntu-26.04.kdl
```

覆盖日、周、月查询范围，上一/下一周期、返回今天、弹窗创建/编辑入口、时间校验、保存后刷新，以及
编辑时间与视图范围隔离。专项测试 5/5。

## Ubuntu 26.04 构建门禁

2026-07-19 使用上述命令完成 Ubuntu 26.04 专项测试、ESLint、生产构建和三语言包校验。

已生成 `coverage/verify-oa-event.lcov`。当前 5 项属于 Vue 源码契约测试，Deno 报告中实际计入的是测试
进程加载的工具配置，因此报告显示的 100% 不代表日程 SFC 的浏览器运行时覆盖率。可验证结果为专项
测试 5/5、ESLint 0 warning、Vite 8 生产构建成功和三语言包完整性通过。
