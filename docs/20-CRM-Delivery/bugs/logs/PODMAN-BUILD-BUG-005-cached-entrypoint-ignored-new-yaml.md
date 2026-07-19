# PODMAN-BUILD-BUG-005：缓存构建镜像未执行新增 YAML 测试开关

日期：2026-07-14

分支：`develop`

状态：已修复

## 现象与影响

主机脚本已传入 `build.bpm_tests` 和 `build.bpm_coverage`，但本地构建镜像仍内置旧入口脚本。
首次运行只完成 Server/Web，若仅查看退出码会误报 BPM 测试已通过。

## 修复与验证

- 使用 YAML 的 `image.rebuild: true` 重建入口镜像，再恢复为日常 `false`；
- 将 BPM 测试放到 Server clean 之后，避免 Surefire 与 JaCoCo 证据被清除；
- 入口会校验“启用覆盖率但未启用测试”的非法组合；
- 重建后日志明确出现 BPM 测试，最终 54 个测试与 CSV 报告均生成。
