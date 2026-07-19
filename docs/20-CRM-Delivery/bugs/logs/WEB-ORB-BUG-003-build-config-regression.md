# WEB-ORB-BUG-003：部分 Ubuntu 构建配置未注入退休媒体源

- 发现：2026-07-16，分支 `develop`
- 现象：使用工单专项 YAML 重建 Web 后，历史 `test.yudao.iocoder.cn/user/avatar/**` 再次进入浏览器，触发 OpaqueResponseBlocking。
- 根因：仅通用 Web 配置配置了 `legacy_media_origins`，多个 CRM 专项 YAML 缺少同一显式配置。
- 修复：所有选择 Web 目标的 YAML 增加退休媒体源；`compile.sh` 在 Web 被选中但未配置时直接失败；数据库兼容迁移同步清理本地历史头像 URL。
- 验证：配置完整性检查、媒体归一化测试和 Ubuntu Web 构建。
