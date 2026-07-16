# CRM 授权统计范围与精确同比环比测试记录

日期：2026-07-15

环境：rootless Podman，Ubuntu 26.04，JDK 17.0.19，Node 22.22.1，pnpm 11.3.0

配置：`podman/config/verify-crm-statistics-authorized-comparison-ubuntu-26.04.yaml`

## 自动化结果

- CRM 全量测试：252/252；
- 统计范围 Service 专项：9/9；
- 统计范围切面与五组 Controller 挂载契约：3/3；
- 员工业绩服务：6/6，包含正负增长、循环小数、零基期和大额十进制；
- 前端统计专项：6/6；
- 统计业绩目录专项 ESLint：零警告；
- Server package、Web production build：通过；
- 完整配置连续执行两次均通过，最终数据取第二次结果。

## 范围场景

| 场景 | 期望 | 结果 |
|---|---|---|
| ALL/CRM 管理员查询所选部门 | 通过 | 通过 |
| SELF 查询本人 | 通过 | 通过 |
| SELF 查询他人 | 拒绝 | `1020014006` |
| 部门树全部人员均已授权 | 通过 | 通过 |
| 父部门夹带未授权子部门人员 | 拒绝 | `1020014006` |
| `userId` 不属于 `deptId` 树 | 拒绝 | `1020014006` |
| 空部门 | 返回空统计，不访问越界数据 | 通过 |
| 组织 API 异常 | 失败关闭 | `1020014006` |
| 缺少登录身份 | 失败关闭 | `1020014006` |

## JaCoCo

| 指标 | 覆盖 |
|---|---:|
| 指令 | 42.59%（11123/26119） |
| 分支 | 40.60%（607/1495） |
| 行 | 40.38%（2082/5156） |
| 方法 | 27.17%（572/2105） |

核心新增及修改类：

- `CrmStatisticsDataScopeService`：指令 125/130、分支 17/20、行 29/30、方法 4/4；
- `CrmStatisticsDataScopeAspect`：指令 32/32、行 8/8、方法 3/3；
- `CrmStatisticsPerformanceServiceImpl`：指令 511/590、分支 36/62、行 95/110、方法 10/12。

覆盖率为 CRM 模块全量口径，原始报告位于构建产物
`Server/mitedtsm-module-crm/target/site/jacoco/`，不提交生成物。

## 真实角色补证

2026-07-16 使用正式系统 API 创建临时 SELF 角色/用户并运行补证。首次暴露组织 API 被当前数据
权限二次过滤的问题，修复后 CRM 自动化为 427/427；本人客户统计与 MySQL 均为 1，同部门他人和
整部门返回 `1020014006`，未授权统计域和跨租户返回 403，匿名返回 401。完整记录见
`testing/crm-statistics-runtime-security/`。
