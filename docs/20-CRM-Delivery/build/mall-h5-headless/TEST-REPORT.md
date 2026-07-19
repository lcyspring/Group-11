# Mall H5 无图形容器构建测试报告

更新日期：2026-07-14

## 环境

- Builder OS：Ubuntu 26.04
- HBuilderX uni-app CLI：5.05.2026032412.0052（Vue3）
- 内置 Node：v18.20.4
- 镜像：`localhost/mitedtsm-hbuilderx-ubuntu:26.04-5.05`
- 镜像 ID：`624b9e643af53cff4b076e13cdb5417e004346d9ed02d81ca1b22a69d9f677a3`
- 镜像大小：556,272,647 bytes

## 已执行验证

| 场景 | 结果 |
|---|---|
| 宿主直接调用真实 uni-app CLI 入口 | 通过 |
| Ubuntu 26.04 精简镜像首次构建 | 通过 |
| `image.rebuild: false` 复用镜像 | 通过 |
| 第二次构建不读取/挂载宿主 HBuilderX | 通过 |
| H5 输出存在且 `index.html` 非空 | 通过 |
| 镜像不含 IDE、GUI CLI、Qt/Xvfb 路径 | 通过 |
| 构建使用 `--network=none` | 通过 |
| H5 输出命中 Git ignore | 通过 |
| 默认 rootless UID 映射生成宿主当前用户产物 | 通过 |

三次容器构建均输出 `DONE Build complete`，当前产物约 7.3 MiB，共 386 个
文件，其中 `assets/` 下 166 个文件。第三次由结构化集成测试执行。

## 覆盖率记录

- 验收行为覆盖：9/9，100%。
- 结构化集成测试断言：CLI 参数契约、脚本语法、真实构建、镜像 OS/组件、
  非图形约束、产物完整性、Git ignore。
- Bash 行/分支覆盖率：未采集；当前没有引入 Bash 覆盖率插桩工具，不能把行为
  覆盖冒充源码覆盖率。后续若脚本逻辑继续增长，再引入 kcov 并设独立门禁。

## 已知非阻塞告警

现有 Mall SCSS 大量使用 Sass `@import`、全局内置函数和旧 JS API，Dart Sass
输出弃用告警但不影响本次产物。这属于前端样式技术债，应独立迁移，避免在容器
改造提交中大范围修改业务样式。
