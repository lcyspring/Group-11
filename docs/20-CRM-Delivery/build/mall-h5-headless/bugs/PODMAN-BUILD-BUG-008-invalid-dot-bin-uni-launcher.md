# PODMAN-BUILD-BUG-008：HBuilderX `.bin/uni` 启动器路径失效

- 发现日期：2026-07-14
- 级别：P1
- 状态：已关闭

## 现象

执行 `uniapp-cli-vite/node_modules/.bin/uni build` 报错：

```text
Cannot find module '../dist/cli/index.js'
```

## 根因

该 `.bin/uni` 是被复制的 launcher，不是保持相对路径语义的软链接；Node 以
`.bin` 为 `__dirname`，从错误位置查找 `../dist/cli/index.js`。

## 修复

入口固定为真实文件：

```text
/opt/HBuilderX/plugins/uniapp-cli-vite/node_modules/@dcloudio/vite-plugin-uni/bin/uni.js
```

容器启动脚本在编译前验证该文件和内置 Node，缺失时立即失败。

## 回归

宿主直接验证和 Ubuntu 26.04 容器构建均通过。
