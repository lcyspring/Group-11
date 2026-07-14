# 2026-07-14 本机观察环境验收

## 文档边界

- 我们新增的 CRM 交付文档统一位于 `docs/20-CRM-Delivery/`；
- 不在项目原有的 `docs/develop/` 下新增或维护交付文档；
- `docs/20-CRM-Delivery/` 作为独立交付目录纳入 `develop` 并随源码上传；
- 本机含凭据的 `podman/config/runtime-local.yaml` 单独忽略。

## 启动方式

```bash
cd podman
bash ./up.sh ./config/runtime-local.yaml
```

运行配置由 YAML 显式提供，命令行参数只指定配置文件路径。运行环境使用
rootless Podman，应用产物由 Ubuntu 26.04 构建容器生成。

## 验收结果

验收时间：2026-07-14（Asia/Shanghai）。

| 检查项 | 地址或对象 | 结果 |
| --- | --- | --- |
| Server 健康检查 | `http://127.0.0.1:8080/actuator/health` | `200`，`status=UP` |
| Web 管理端 | `http://127.0.0.1:8081/` | `200 OK` |
| Mall 前端 | `http://127.0.0.1:8082/` | `200 OK` |
| 管理端登录接口 | `/admin-api/system/auth/login` | `code=0`，成功签发令牌 |
| 客服工单闭环 | `/admin-api/crm/work-order/*` | 创建、开始、完结、轨迹、通知和待办通过 |
| 工单统计 | `/admin-api/crm/statistics-work-order/*` | 五个接口均 `code=0` |
| 客户 360 工单记录 | `/admin-api/crm/work-order/page?customerId=17` | `code=0`，返回 2 条匹配工单 |
| 工单手工分派 | `/admin-api/crm/work-order/assign` | `W-202607-0003` 从用户 100 分派至 145，轨迹和通知通过 |
| CRM 发票生命周期 | `/admin-api/crm/invoice/*` | 草稿、开具、部分/全部红冲、红票作废恢复、超额拒绝和轨迹通过 |
| Pod | `mitedtsm-rootless` | 运行中 |
| 基础设施 | MySQL、Redis、RabbitMQ、TDengine | 均运行中 |
| 应用容器 | Server、Web、Mall | 均运行中 |

管理端观察账号来自项目初始化 SQL 与认证接口示例，并已经实际登录验证：

- 租户 ID：`1`
- 用户名：`admin`
- 密码：由本机显式配置或初始化数据提供，不写入可上传文档。

## 本机代理注意事项

本机代理环境会将普通 `curl` 的本地请求错误转发到代理端口。命令行复核本地
服务时应显式使用 `curl --noproxy '*' ...`。浏览器直接打开上述回环地址不受本
记录中的命令行代理问题影响。

## 停止方式

观察结束后执行：

```bash
cd podman
bash ./down.sh ./config/runtime-local.yaml
```

当前环境暂不停止，保留给人工观察。

## 本次增量

- Web：已包含客服工单列表、表单、详情轨迹和待办入口；
- 运行样例：`W-202607-0001` 已完结，`W-202607-0002` 处理中并保留在待办；
- MySQL 通过 `mysql.timezone` 显式使用 Asia/Shanghai，更新时间不再回退 8 小时；
- Pod 重建先停止 Server 再停止基础设施，顺序停机复验未触发强杀。
- 已补执行合同来源字段幂等迁移，待审核回款与回款提醒恢复；
- Web 已用 Ubuntu 26.04 重建并热替换，默认不再访问百度统计域名；
- `up.sh` 后续会在 Server 启动前执行 YAML 指定的兼容迁移，避免持久卷结构再次漏更。
- Web 最终产物已通过 Ubuntu 26.04 构建并使用 `rebuild-web` 热替换；
- 热替换后 Server 健康为 `UP`，Web/Mall 均为 `200 OK`，工单统计和客户筛选接口无持续 502；
- CRM 当前回归 170/170，通过率 100%，JaCoCo 行覆盖率 32.64%。
- `rebuild-server` 已在停止旧服务前执行兼容迁移，分派权限和新 Server 同批生效；
- Web 已再次热替换，工单多维筛选和分派弹窗产物在 8081 提供观察。
- CRM 合同产品快照迁移已由 `rebuild-server` 执行并重复运行验证幂等；
- 运行样例合同 21 在产品改名、改编码、改单位、改价并下架后仍返回成交时快照；
- CRM 当前回归 173/173，JaCoCo 行覆盖率 33.33%，Web/Server 均通过 Ubuntu 26.04 构建并热替换。
- CRM 发票后端回归并入当前 193/193 基线；发票前端专项 3/3，目录 ESLint 0 警告；
- 发票日期契约缺陷 `CRM-INV-BUG-003` 已修复，Ubuntu 26.04 Web 生产产物完成热替换；
- 合同 13 的运行样本完成金额守恒：100.00 蓝票、红票作废恢复、100.00 全额红冲后净额归零；
- 16016.01 超合同额度请求返回业务码 `1020016006`，未写入草稿。
- CRM 合同 13 已完成签署、相同命令幂等、签署副本锁定和作废闭环；轨迹动作 7/8、序号 1/2 正确；
- 生命周期摘要接口不再返回合同/产品完整快照，签署副本下载为 `200`；
- `local-record` 能力只暴露线下签署，前后端均不再把本地事实登记显示为可用电子签平台；
- CRM 当前回归 201/201，JaCoCo 行覆盖率 37.00%，合同生命周期目录 ESLint 0 警告。
