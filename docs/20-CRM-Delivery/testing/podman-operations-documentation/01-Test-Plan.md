# Podman 操作与字段文档测试计划

1. 检查运行、Server/Web 构建和 Mall H5 三类 YAML 均有字段章节；
2. 检查 `rebuild-server/web/mall`、`full`、`check` 等模式均有明确适用场景；
3. 检查编译、镜像打包、部署、停止和归档脚本职责不混淆；
4. 检查 H5 产物明确为本地生成且 Git 忽略；
5. 执行 runtime-config 门禁，确保文档使用的入口仍遵守单 YAML 参数契约。
