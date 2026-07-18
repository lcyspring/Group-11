# 客户画像临时数据验收记录

日期：2026-07-17。分支：`develop`。

通过 `podman/tests/acceptance/verify-crm-customer-portrait-runtime.sh` 和显式 YAML 在当前 rootless Podman 运行环境构造 4 条唯一前缀临时客户。脚本登录真实管理端 API，对行业、来源、级别、生命周期、地区汇总及钻取逐项断言，并通过退出清理钩子删除临时客户及其对象权限。

最终输出：`dimensions=3 lifecycle=4 area-drill=4`；清理后数据库前缀计数为 `0`。该脚本可重复执行，不依赖或修改现有演示客户。
