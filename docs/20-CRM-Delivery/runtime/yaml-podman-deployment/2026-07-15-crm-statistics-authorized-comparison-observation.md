# 2026-07-15 CRM 授权统计与同比环比观察环境

## 构建与部署

- 分支：`develop`；
- 构建配置：`podman/config/verify-crm-statistics-authorized-comparison-ubuntu-26.04.yaml`；
- 部署配置：`podman/config/runtime-local.yaml`；
- Ubuntu 26.04 Server package、CRM 252/252、前端专项 6/6、专项 ESLint、Web production build 全部通过；
- rootless Podman 使用保留数据库等持久卷的 full 模式重建 Server 和 Web 运行镜像及整个 Pod。

## 真实 API 验收

使用租户 1 的用户 1。该用户的平台角色数据范围包含 ALL，但不持有 YAML 配置的 `crm_admin` 角色，
因此允许结果来自组织 ALL 范围；人员与部门不一致仍会被统一统计范围门拒绝。

| 场景 | 结果 |
|---|---|
| 部门 100，2026 年合同数量统计 | `code=0`，返回 12 个月，包含两个增长率字段 |
| 部门 107 但显式选择部门外用户 103 | `code=1020014006`，拒绝 |
| 部门 100，2024 年合同金额 | 2024-02 为 34314.24，零基期的同比/环比为 `null` |
| 同一序列 2024-03 | 本期 0、上月 34314.24，服务端环比为 `-100.00` |

验收只登录和读取统计数据，未修改业务数据。SELF、部门全覆盖、未授权子部门、API 异常和缺失身份由
专项单元测试覆盖。

## 当前观察地址

- Server：`http://127.0.0.1:8080/actuator/health`；
- Web：`http://127.0.0.1:8081/`；
- Mall：`http://127.0.0.1:8082/`。

Server 为 `UP`，Web/Mall 均为 HTTP 200。服务保持运行，管理端员工业绩的合同数量、合同金额和回款金额
图表均直接消费 Server 返回的同比/环比字段。
