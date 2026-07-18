# Podman 内部实现

本目录只保存 `compile.sh` 调用的标准工具链助手，以及在构建容器内部执行的入口脚本。其他成员
不应直接调用这里的文件；公开编译命令始终是：

```bash
bash podman/compile.sh <config.yaml>
```
