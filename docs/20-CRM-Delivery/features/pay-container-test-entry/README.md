# CRM-FEATURE-068：Pay 容器测试与覆盖率入口

状态：已实现。分支：`develop`。日期：2026-07-18。

Pay 模块测试已进入统一的 `compile.sh + KDL` 构建协议。使用者只传配置文件路径，由 KDL 显式选择
Pay 测试、JaCoCo 和受限的 Surefire 类模式；Host 不需要 JDK、Maven，也不在 Host 安装依赖。

标准入口：

```bash
cd podman
bash ./compile.sh ./config/test-pay-ubuntu-26.04.kdl
```

该入口优先使用公共工具链镜像
`ghcr.io/elel-code/group-11-build-ubuntu:26.04`。Maven 依赖在容器运行时解析并进入命名卷缓存，
不烘焙进工具链镜像。覆盖率证据固定输出到
`Server/mitedtsm-module-pay/target/site/jacoco/`。

配置约束：开启 `pay_coverage` 时必须同时开启 `pay_tests`；`pay_test_pattern` 只接受测试类名模式，
不能注入任意 Maven 参数。编译、运行镜像构建和部署仍是三个独立阶段。
