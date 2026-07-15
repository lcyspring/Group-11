# 2026-07-15 CRM 统一授权观察环境

## 构建与部署

- 分支：`develop`；
- 构建配置：`podman/config/verify-crm-authorization-ubuntu-26.04.yaml`；
- 部署配置：`podman/config/runtime-local.yaml`；
- Ubuntu 26.04 Server package、CRM 239/239、专项 ESLint、Web production build 全部通过；
- rootless Podman 使用保留持久卷的 full 模式重建运行镜像和 Pod。

## 真实权限验收

使用租户 1 的现有用户 1。该用户的平台角色数据范围包含 ALL，但不持有 YAML 指定的 `crm_admin` 角色，
因此本次结果来自组织范围算法，不是 CRM 管理员对象权限绕过。

| 场景 | 结果 |
|---|---|
| 客户 `sceneType=1` 我负责 | `code=0`，总数 1 |
| 客户 `sceneType=4` 组织范围 | `code=0`，总数 6 |
| 读取客户 12（负责人 103） | `code=0`，组织 READ 生效 |
| 导出 `sceneType=4` 全部客户 | `code=1020007010`，组织 READ 不提升为导出权限 |

验收未修改业务数据。部门范围、自定义部门、SELF、空响应、下属交集和直接对象权限层级由 7 项专项单测
覆盖；真实 ALL 场景验证了列表、详情和导出的完整边界。

## 当前观察地址

- Server：`http://127.0.0.1:8080/actuator/health`；
- Web：`http://127.0.0.1:8081/`；
- Mall：`http://127.0.0.1:8082/`。

Server 为 `UP`，Web/Mall 均为 HTTP 200。服务保持运行供浏览器观察；管理端线索、客户、联系人、
商机、合同、回款计划、回款、退款和发票列表均已显示“组织范围”入口。
