# dasel/KDL 配置测试结果

执行日期：2026-07-18。

| 检查 | 结果 |
|---|---|
| dasel 固定 tag/commit 本地静态编译与版本检查 | 通过，`3.11.2` |
| 正常 KDL 全量解析 | 通过，157/157 |
| Git 旧 YAML 与新 KDL 结构化等价比较 | 通过，125/125（路径后缀按迁移规则归一化） |
| 重复节点负向 fixture | 通过，已拒绝 |
| 第三层结构负向 fixture | 通过，已拒绝 |
| 全部 Podman Shell `bash -n` | 通过 |
| 演示数据生成及负向参数契约 | 通过 |
| deploy/stop/build-images/build-image-archives check | 通过，无状态 |
| 完整 runtime-config 门禁 | 通过，Pod 保持 Running |

本轮未执行完整应用编译、镜像重建和数据集替换；这些属于后续三阶段验收，不计为本项失败。
