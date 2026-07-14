# PODMAN-BUILD-BUG-007：HBuilderX offscreen 主程序异常退出

- 发现日期：2026-07-14
- 级别：P2
- 状态：已关闭

## 现象

在 Ubuntu 容器中设置 `QT_QPA_PLATFORM=offscreen` 后启动 HBuilderX 主程序，
进程 abort；切换 Xvfb 又引入 Qt、X11、GL、字体等大量无关运行依赖。

## 根因

H5 编译不要求 IDE 主进程。将 GUI 主程序作为 CLI 服务前置条件，选错了编译
入口，并把桌面运行时耦合进了 builder。

## 修复

直接使用 `@dcloudio/vite-plugin-uni/bin/uni.js build`。最终镜像不复制
`HBuilderX` 主程序和 GUI `cli`，也不安装 Xvfb。

## 回归

Ubuntu 26.04 容器内完成两次真实 H5 构建；镜像检查确认不存在 IDE、GUI CLI
和 `/usr/bin/Xvfb`。
