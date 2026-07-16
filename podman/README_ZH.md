# Rootless Podman 中文说明

本项目使用 rootless Podman，不使用项目原有 Docker、Docker Compose 或 Docker socket。编译、测试、
镜像打包、部署、备份和恢复入口的命令行都只接受一个 YAML 配置路径。

## 快速导航

- [编译、构建与部署操作手册](OPERATIONS_ZH.md)
- [完整部署指南](DEPLOY_GUIDE_ZH.md)
- [YAML 全字段参考](config/YAML_FIELDS_ZH.md)
- [配置文件分类](config/README_ZH.md)
- [镜像归档说明](images/README_ZH.md)
- [各镜像来源、作用与归档策略](images/README_ZH.md#镜像来源与作用)

## 日常命令

```bash
cd /path/to/Group-11

# Server、InitService、管理端、CRM 测试和 JaCoCo
bash ./podman/build-in-ubuntu.sh ./podman/config/build-ubuntu-26.04.yaml

# Mall H5 无图形构建
bash ./podman/build-mall-h5-in-ubuntu.sh ./podman/config/build-mall-h5-ubuntu-26.04.yaml

# 无状态预检
bash ./podman/tests/runtime-config/run.sh ./podman/config/runtime-local-check.yaml

# 部署；具体模式只在 YAML 中修改
bash ./podman/up.sh ./podman/config/runtime-local.yaml
```

## 产物与镜像

- 后端产物：`Server/mitedtsm-server/target/mitedtsm-server.jar`；
- 管理端产物：`Web/dist-prod/`，进入独立 Nginx 镜像；
- Mall H5：`MallFrontend/unpackage/dist/build/web/`，本地生成且 Git 忽略，进入独立 Nginx 镜像；
- 前端不会打进后端 JAR/WAR；
- Ubuntu Server/Web 与 HBuilderX 编译工具链镜像推荐 save 或上传 OCI 仓库；
- Server/Web/Mall 项目运行镜像推荐由当前源码产物重建。

## 配置分类

- 日常构建/部署：`build-ubuntu-26.04.yaml`、`build-mall-h5-ubuntu-26.04.yaml`、
  `runtime-local-check.yaml` 和被忽略的 `runtime-local*.yaml`；
- 专项测试：`verify-*`、`test-*`、`check-*`，用于复现功能验收，不是普通启动配置；
- 数据保护：`database-backup-check.yaml` 复制为 ignored 本机配置后执行；
- 编译镜像：`build-image-archives-check.yaml` 复制为 ignored 本机配置后选择 save/load/push。
- 性能验收：`verify-crm-performance-baseline.example.yaml` 复制为 ignored 本机配置后执行只读负载。

## 安全边界

- `runtime-local-check.yaml` 不改变 Pod/卷；
- 删除数据卷必须在 YAML 同时选择 stop 和 `remove_volumes_on_down: true`；
- 恢复覆盖真源库必须二次授权且 Server 已停止；
- 真实密码只写被 Git 忽略的本机 YAML；
- 编译镜像 tar、备份文件和 checksum 均被 Git 忽略，不提交仓库。
