# PODMAN-BUILD-BUG-002：非 UTF-8 locale 导致 javac 扫描失败

更新日期：2026-07-14

## 现象

Ubuntu 最小镜像中的 Maven 编译在扫描源码目录时失败：

```text
InvalidPathException: Malformed input or input contains unmappable characters
```

失败路径包含中文 Markdown 文件名；相同源码在 UTF-8 宿主环境可以扫描。

## 根因

最小 Ubuntu 镜像没有显式 UTF-8 locale。JVM 使用不可表示中文路径的默认字符集，`Path` 转换在进入实际 Java 编译前失败。

## 修复

在专用构建镜像中显式设置：

```dockerfile
LANG=C.UTF-8
LC_ALL=C.UTF-8
```

## 验证

重新构建镜像后，同一 Server Maven 命令已越过原失败目录，并继续编译后续模块。

## 状态

已关闭；Server 37 模块完整构建通过。
