# KDL 显式配置迁移实施记录

实施日期：2026-07-18。目标分支：`develop`。

## 决策与当前状态

Podman 编译、运行镜像封装、部署、数据库、BPM、诊断和验收入口已统一采用 KDL，命令行继续只接收
一个 `.kdl` 配置路径。仓库不再维护 YAML/KDL 双份真源，也不再接受 `.yaml` 兼容入口。

KDL 由项目本地固定版本的 dasel `v3.11.2` 原生解析，源码固定为上游 commit
`008b0ed9cae7d5d5b0c72e23c84836c5b2f0338b`。`podman/tools/build-dasel.sh` 负责复现静态二进制，
运行脚本固定使用 `podman/tools/bin/dasel`，不会因 Host PATH 中存在其他版本而漂移。

## 配置契约

```kdl
schema_version 1

operation {
  startup_mode "replace-server"
}

mysql {
  database "mitedtsm_database"
  dataset "crm-demo-v2"
  dataset_mode "preserve"
  dataset_manifest "../../database/generated/crm-demo-v2/crm-demo-v2.manifest"
}
```

- 只允许根标量与一层分组标量；
- 重复节点、数组、空值和第三层结构在任何业务动作前统一失败；
- 布尔使用 `#true/#false`，字符串显式加引号；
- 相对路径始终以配置文件目录为基准；
- 示例配置提交 Git，真实账号、密码和部署地址只写 ignored 本机 KDL；
- 业务入口只调用 `kdl_get/kdl_require/kdl_bool/kdl_path`，不自行解析配置文本；
- 测试需要临时改写 KDL 时调用 dasel 驱动的 `kdl_set_file`，不再使用 YAML `sed/awk` 替换。

## 已实施

1. 159 份 Podman 配置和测试 fixture 已从 `.yaml` 转换为 `.kdl`；
2. 运行读取层已删除手写 YAML/KDL AWK 解析，改为 dasel 解析一次并执行严格结构校验；
3. 47 个编译、部署、运维和验收脚本已切换到 KDL API；
4. 数据库 provision、营销 Provider 和演示数据生成测试已改用 dasel 修改临时配置；
5. 当前 README、操作指南、字段参考和配置目录说明已切换到 KDL；
6. 全部 157 份正常 KDL 已通过 dasel/结构契约校验，重复节点与非法深度 fixture 均被拒绝；
7. 部署、停服、运行镜像封装和工具链镜像归档的无状态 `check` 已通过。

完整 Ubuntu 26.04 编译、运行镜像重建和有状态替换仍按三阶段门禁单独执行，不能与配置解析验证混为一次
破坏性操作。
