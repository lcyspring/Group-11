# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-ubuntu-26.04.yaml
```

- `CrmContractServiceImplTest`：6/6；
- CRM 全量：111/111；
- Failures：0；Errors：0；Skipped：0；
- Maven reactor：20/20 SUCCESS。

专项覆盖未赢单拒绝、通用入口隔离、已有合同复用、继承客户/负责人、并发胜出合同复用及
WRITE 对象权限注解契约。

## MySQL 8

- 使用本机已有 `docker.io/library/mysql:8.0` 镜像；
- 加载完整 `crm-2024-09-30.sql` 历史基线；
- 基线中 `business_id=13` 的两份有效合同迁移后仍为 2；
- 新迁移连续执行两次成功，唯一索引保持 1 个；
- 第二份 `tenant=1/source_business_id=999` 插入返回 MySQL `1062`；
- 第一份逻辑删除后重新插入成功，最终历史 2、有效 1；
- 验证容器已删除。

## Ubuntu 26.04 Web

- Prettier：通过；
- ESLint：通过；
- CRM 统计纯函数：7/7；
- Vite production build：成功；
- 未使用宿主机 JDK、Node 或 pnpm。
