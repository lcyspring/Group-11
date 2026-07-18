# PODMAN-BUILD-BUG-009：Pay 测试未进入单 YAML 标准入口

日期：2026-07-18。分支：`develop`。优先级：P1/工程。状态：已关闭。

## 现象

Pay 全量测试和 JaCoCo 只能依靠人工拼接 Maven 命令执行。`compile.sh` 的 YAML 已支持 CRM、BPM、
Infra 等模块，却不能显式选择 Pay，其他成员无法用项目规定的容器入口复现测试证据。

## 根因

标准编译入口、容器环境变量传递和 Ubuntu 构建入口均缺少 Pay 选择器；字段文档和示例配置也没有
Pay 测试契约。测试修复虽然存在，但尚未成为可重复门禁。

## 修复关键

- 新增 `build.pay_tests`、`build.pay_coverage` 和受字符白名单约束的 `build.pay_test_pattern`；
- 将字段从 Host 入口传入仓库当前版本的容器入口，避免缓存镜像静默忽略新逻辑；
- 在 Ubuntu 26.04 容器中执行 Pay reactor 测试并生成 JaCoCo；
- 覆盖率开启但测试关闭时失败关闭；报告缺失时构建失败；
- coverage 任务本身也会激活标准入口，并统一前置校验八类模块的“覆盖率依赖测试”约束；
- 提交可复现的 `test-pay-ubuntu-26.04.yaml` 和中文字段说明。

## 验证

- 单 YAML 入口 `BUILD SUCCESS`，reactor 19/19 SUCCESS；
- Pay 167 个，132 通过、35 个外部集成跳过、失败 0、错误 0；
- JaCoCo 指令 23.81%、分支 18.74%、行 24.57%、方法 22.96%；
- Host 未使用 JDK、Maven 或 Host `node_modules`。
- `pay_coverage=true`、`pay_tests=false` 的空目标负样本稳定返回配置错误，不再提前退出为普通空选择。
