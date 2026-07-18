# 顶部站内信弹层可靠性测试

日期：2026-07-15

环境：rootless Podman，Ubuntu 26.04，Node 22.22.1，pnpm 11.3.0

配置：`podman/config/verify-web-notification-ubuntu-26.04.kdl`

## 自动化结果

- `notificationLoader.test.mjs`：3/3；
- 通知组件目录 ESLint：零警告；
- Web production build：通过。

## 覆盖场景

| 场景 | 断言 |
|---|---|
| 首个请求未完成时连续加载 | 复用同一 Promise，Fetcher 只调用一次 |
| 已有最近成功列表 | 普通加载立即返回缓存，不重复访问接口 |
| 弹层显式刷新 | 请求新数据并原子替换缓存 |
| 首次请求失败 | 进行中状态正确释放，下一次可以重试 |
| 用户状态清理 | 最近成功缓存清空，不向下个会话泄漏 |

## 运行证据

- Server 日志：`get-unread-list` 查询返回 1 条，接口耗时约 11 ms；
- `get-unread-count` 返回 1，接口耗时约 12 ms；
- 修复不修改消息记录，弹层打开不调用标记已读接口；
- 生产构建产物由 Ubuntu 26.04 容器生成。
