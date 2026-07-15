# PODMAN-BUILD-BUG-008：缓存工具链镜像静默跳过新增测试开关

- 发现/关闭日期：2026-07-15
- 级别：P1 / 构建证据完整性

## 现象与根因

YAML 已启用 `infra_tests` 和 `infra_coverage`，主机脚本也传入环境变量，但本地镜像仍内置旧版入口脚本。
构建以 0 退出却没有 Infra Surefire/JaCoCo 产物，容易误报安全测试已通过。每次脚本变化都要求重建完整
Ubuntu 工具链镜像，既慢又容易遗漏。

## 修复关键

- 构建镜像只承载 Ubuntu 26.04、JDK、Maven、Node 和 pnpm；
- `podman run` 通过 `/bin/bash` 执行仓库挂载目录中的当前入口脚本；
- 命令行仍只传 YAML 路径，镜像无需因流水线脚本变化而重建；
- Infra JaCoCo 排除 JSqlParser 超大生成方法，避免无关插桩噪声。

## 验证

复用未重建的 `localhost/mitedtsm-build-ubuntu:26.04` 后，日志明确执行 Infra 文件测试，28/28 通过并生成
JaCoCo CSV；随后 CRM 231/231、专项 ESLint 和 Web production build 通过。
