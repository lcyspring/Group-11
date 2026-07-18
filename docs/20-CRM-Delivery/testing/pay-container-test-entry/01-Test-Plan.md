# Pay 容器测试入口测试计划

日期：2026-07-18。分支：`develop`。

1. 只以 KDL 路径调用 `compile.sh`，不依赖 Host JDK/Maven；
2. 使用公共 Ubuntu 26.04 工具链镜像，依赖仅在容器运行时解析；
3. `pay_tests=true` 执行 Pay 模块测试，测试模式不能成为 Maven 参数注入入口；
4. `pay_coverage=true` 生成 Pay 模块 `jacoco.csv`；
5. 覆盖率开启但测试关闭时立即拒绝配置；
6. Pay 测试结果必须为失败 0、错误 0；
7. Shell 语法、KDL 字段文档和运行配置静态门禁保持通过。
