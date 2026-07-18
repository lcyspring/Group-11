# CRM 发票生命周期测试记录

执行日期：2026-07-14。环境：rootless Podman、Ubuntu 26.04、OpenJDK 17、pnpm 11.3.0、MySQL 8.0。

## 结构化测试范围

| 层次 | 覆盖项 |
|---|---|
| VO | 正金额、邮箱、创建/修改身份字段分离、正式票号/日期、红冲/作废原因 |
| Service | 合同审批、客户/负责人继承、专票信息、草稿可变/正式票不可变、字段白名单 |
| 金额 | 合同净额、剩余额度、部分/全部红冲、超额拒绝、红票作废恢复 |
| 并发/幂等 | 合同后票据锁顺序、稳定 issue/red/void 请求号、适配器错误确认回滚 |
| 权限/审计 | 合同/发票对象权限注解、红票对象权限、生命周期轨迹 |
| Provider | local-ledger 三类命令不伪造外部票据且原样返回请求号 |
| 前端 | 状态动作矩阵、红冲剩余额度精度和非负保护、Epoch 毫秒命令日期契约 |
| MySQL | 两表创建、8 项权限、三语言菜单、6 个 CHECK、迁移连续执行两次 |

后端专项代码：

- `Server/mitedtsm-module-crm/src/test/java/com/meession/etm/module/crm/service/invoice/`
- `Server/mitedtsm-module-crm/src/test/java/com/meession/etm/module/crm/controller/admin/invoice/vo/`
- `Server/mitedtsm-module-crm/src/test/java/com/meession/etm/module/crm/framework/invoice/`

前端专项代码：`Web/src/views/crm/invoice/constants.test.mjs`。

## 运行入口

后端与 JaCoCo：

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.yaml
```

前端专项：

```bash
cd podman
bash ./compile.sh ./config/test-crm-invoice-web-ubuntu-26.04.yaml
```

发票目录专项静态检查：

```bash
cd podman
bash ./compile.sh ./config/check-crm-invoice-web-ubuntu-26.04.yaml
```

生产前端构建：

```bash
cd podman
bash ./compile.sh ./config/build-web-ubuntu-26.04.yaml
```

## 执行结果

- CRM 后端当前基线：193/193，失败 0、错误 0、跳过 0；发票相关新增 19 条。
- CRM 发票前端纯函数：3/3。
- Web Ubuntu 26.04 生产构建：通过。
- MySQL 迁移：同一真实 MySQL 数据卷连续执行两次均通过；表、权限、三语言和 CHECK 已查询确认。

## 运行时 API 闭环

在合同 13（已审批、合同金额 16016.00）上完成真实接口验收：

| 步骤 | 结果 |
|---|---|
| 创建 100.00 草稿并正式开具 | 成功，生成 `FP202607-0001` |
| 部分红冲 30.00 | 净额 70.00、剩余额度 15946.00 |
| 作废 30.00 红票 | 原蓝票恢复已开具，净额恢复 100.00 |
| 全额红冲 100.00 | 原蓝票变为全部红冲，净额 0、剩余额度恢复 16016.00 |
| 创建 16016.01 超额草稿 | 业务码 `1020016006` 拒绝，未写入 |
| 数值时间戳落库 | `1784028120000` 返回值不变，MySQL 为 `2026-07-14 19:22:00` |

详情轨迹同时验证 Provider 请求号 `invoice:issue:1`、
`invoice:red:1:E2E-RED-20260714-002` 和 `invoice:void:2` 均可追溯。格式化日期静默写成
Unix 纪元的问题在该轮验收中发现并修复，见 `CRM-INV-BUG-003`。

运行时入口状态：Server `UP`，Web、Mall 均为 `200 OK`。Web 修复产物已通过
`startup_mode: rebuild-web` 热替换，服务保持在 8080/8081/8082。

## JaCoCo

| 指标 | 覆盖 | 已覆盖/总计 |
|---|---:|---:|
| 指令 | 38.29% | 8742/22830 |
| 分支 | 35.98% | 458/1273 |
| 行 | 36.12% | 1624/4496 |
| 方法 | 24.53% | 297/1211 |

发票核心 `CrmInvoiceServiceImpl` 覆盖 992 条指令、35 个分支、197 行和 23 个方法；
`LocalLedgerCrmInvoiceProvider` 为 29/29 指令、5/5 行、5/5 方法。原始 HTML/CSV 位于
`Server/mitedtsm-module-crm/target/site/jacoco/`，作为构建产物不提交。
