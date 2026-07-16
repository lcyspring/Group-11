# 测试计划

日期：2026-07-16。环境：Ubuntu 26.04 构建镜像、rootless Podman、MySQL 8.0。

## 自动化范围

- 处理组保存、负责人必须为成员、成员顺序和有关联工单禁止删除。
- YAML 属性绑定与非法配置拒绝。
- 自动派单最小负载、稳定决胜、无候选进入未分配池。
- 非成员领取拒绝、并发领取原子保护、组负责人/普通成员/抄送人读取边界。
- 本组派单与跨组 `assign-all` 边界、候选人范围和服务端拒绝路径。
- 抄送去重、上限、关系持久化、通知以及完结通知。
- 工单分页的优先级排序、抄送视图和处理组未分配视图。
- Web 领取判断、抄送 ID 归一化、候选人负载标签、专项 ESLint 和 production build。

## 真实 API 范围

- 处理组创建、更新、列表和成员回显。
- 基于真实开放工单数的自动派单。
- 重复抄送去重、抄送通知和“抄送我的”视图。
- 无候选进入未分配池、组成员领取、领取后退出未分配视图。
- 改派、开始、完结和轨迹动作。
- 验收数据、通知、轨迹、关系和临时负载种子零残留。

执行入口：

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-work-order-collaboration-ubuntu-26.04.yaml
bash ./verify-crm-work-order-collaboration.sh ./config/verify-crm-work-order-collaboration-local.yaml
```
