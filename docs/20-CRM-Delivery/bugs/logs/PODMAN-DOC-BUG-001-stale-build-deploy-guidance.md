# PODMAN-DOC-BUG-001：构建部署文档模式缺失且 H5 产物说明过期

## 现象

README 未列出 `rebuild-server`，中文指南仍称 Mall H5 构建产物纳入版本管理，也没有逐字段解释新增
安全、集成、文件和 TDengine 配置。成员无法仅凭文档可靠判断“编译、镜像打包、部署”的边界。

## 修复

- 增加阶段职责、标准流程和按变更选择启动模式的操作手册；
- 增加运行、Ubuntu 构建和 Mall H5 YAML 全字段参考；
- 明确前端进入独立 Nginx 镜像，后端为 JAR；
- 明确 H5 输出本地生成且 Git 忽略，补齐 `rebuild-server` 和安全验收入口。

## 分支

`develop`
