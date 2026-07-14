# 2026-07-15 CRM 退款与字符集修复观察环境

## 启动与构建

- 分支：`develop`；
- 构建：Ubuntu 26.04 容器，配置 `podman/config/verify-crm-refund-ubuntu-26.04.yaml`；
- 运行：rootless Podman，配置 `podman/config/runtime-local.yaml`；
- 命令行只传 YAML 路径，配置文件中的 `startup_mode` 最终保持 `full`。

## 可观察服务

| 服务 | 地址 | 结果 |
|---|---|---|
| Server | `http://127.0.0.1:8080/actuator/health` | `status=UP` |
| Web | `http://127.0.0.1:8081/` | 200 |
| Mall | `http://127.0.0.1:8082/` | 200 |
| MySQL、Redis、RabbitMQ、TDengine | Pod `mitedtsm-rootless` | 全部运行 |

本机代理可能错误接管回环请求；命令行检查应使用 `curl --noproxy '*'`，浏览器可直接访问。

## CRM 退款运行结果

- 管理端登录成功签发令牌；
- 退款分页接口返回 `code=0`；
- 原回款 29 可退摘要：原金额 16016、占用 0、可退 16016；
- 前一轮创建/删除和超额拒绝样本已清理，主表和动作轨迹当前有效记录均为 0；
- 审批流程定义尚未部署，观察环境不伪造提交成功状态。

## 字符集复核

- 全量启动真实执行 compatibility manifest，包含退款迁移和 UTF-8 修复迁移；
- 新增菜单 ID 6000+ 双重编码特征计数为 0；
- `客服工单`、`发票管理`、`退款/冲销管理` 的 `HEX(name)` 均为正确 UTF-8；
- 菜单、权限、字典、通知和中/英/阿翻译均已恢复。

环境继续保持运行，供浏览器人工观察。
