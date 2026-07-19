# Deno 与 Vite 8 工具链迁移测试结果

日期：2026-07-19。环境：rootless Podman、Ubuntu 26.04、Deno 2.9.3、Vite 8.1.5。

- Web 媒体/图标专项：5/5，通过；
- 离线图标：40 个集合、2051 个图标，三个远程 API 域名在产物中为 0；
- Web production build：6807 modules，47.16 秒，通过，warning 0；
- 三语分包：中文、英文、阿拉伯文唯一且从生产入口可达；
- 纯 ESLint 配置：通过，不创建 coverage profile；
- Mall H5：7/7 媒体测试、构建成功、167 个资产；结构化断言 10/10；
- pnpm 历史缓存：Web/Mall 两卷均已检测、清理并由 Deno lock 重建；
- runtime-config：通过，Pod 保持 Running 且 ID 不变；
- documentation：56 个相对链接通过，退休入口 0；
- `git diff --check`：通过。
