# CRM 统一授权范围测试记录

日期：2026-07-15

环境：rootless Podman，Ubuntu 26.04，JDK 17.0.19，Node 22.22.1，pnpm 11.3.0

配置：`podman/config/verify-crm-authorization-ubuntu-26.04.yaml`

## 自动化结果

- CRM 测试：239/239；
- 新增 `CrmAuthorizationServiceTest`：7/7；
- 覆盖 SELF 失败收敛、部门用户转换、ALL、直接权限层级、组织只读、下属交集和管理员例外；
- `CrmPermissionServiceImplTest` 验证下属 OWNER 不能替代当前用户直接导出权限；
- 专项前端 ESLint：零警告；
- Server package、Web production build：通过；
- 首次全量构建捕获防御空列表代码使用了错误的 Hutool 重载，按 `AdminUserApi` 的 `List` 契约修正后，
  同一配置从头重跑全部通过，失败产物未进入提交。

## JaCoCo

| 指标 | 覆盖 |
|---|---:|
| 指令 | 42.13%（10915/25905） |
| 分支 | 39.82%（585/1469） |
| 行 | 39.82%（2033/5105） |
| 方法 | 26.61%（554/2082） |

核心新增类：

- `CrmAuthorizationService`：指令 182/190、分支 26/34、行 37/39、方法 9/9；
- `CrmOwnerReadScope`：指令 28/31、分支 3/6、行 5/5、方法 2/2。

覆盖率为 CRM 模块全量口径，不以新增代码局部高覆盖替代整体指标。
