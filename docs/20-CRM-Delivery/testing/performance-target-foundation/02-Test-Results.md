# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.yaml
```

- Tests：98/98；
- Failures：0；
- Errors：0；
- Skipped：0；
- Maven reactor：20/20 SUCCESS；
- 新增目标请求校验 3 项、目标服务 3 项、汇总契约 1 项。

## MySQL 8 迁移验证

- 使用本机已有 `docker.io/library/mysql:8.0` 镜像启动一次性 Podman 容器；
- 对 `new-crm-performance-target.sql` 连续执行两次，均成功；
- 维护/删除权限保持 2 条，三语菜单保持 6 条；
- 同一目标逻辑删除后重新创建成功；
- 验证结束后测试容器已删除，没有保留测试数据库或容器。

## 静态检查

- `bash -n podman/init/init-mysql.sh`：通过；
- `git diff --check`：通过；
- 新增 Java、SQL、Markdown：UTF-8/ASCII。
