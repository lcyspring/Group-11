# KDL 显式配置迁移 Plan

制定日期：2026-07-18。目标分支：`develop`。

## 决策

项目后续配置语言目标调整为 KDL。该决定基于配置结构、类型明确性、可编辑性和领域表达能力；
TOML 不进入候选，因为本项目包含重复服务节点、分阶段操作、嵌套策略和数据集声明，使用表与数组表表达会
使结构分散，难以形成清晰的编译、构建、部署领域配置。

命令行契约不改变：每个入口仍只接收一个配置文件路径。操作类型由脚本与配置共同确定，不增加环境变量、
位置参数或隐式默认值作为第二套配置来源。

## 目标形式

```kdl
schema-version 1

operation startup-mode="replace-server"

mysql database="mitedtsm" dataset="crm-demo-v2" dataset-mode="preserve" {
    dataset-manifest "../../database/generated/crm-demo-v2/crm-demo-v2.manifest"
}

dataset-generation name="crm-demo-v2" seed=20260718 tenant-id=1 owner-user-id=1 {
    demo-user-count 8
    demo-user-password-source "owner"
    time start="2025-07-01" end="2026-07-18"
    count customers=120 businesses=180 stages=5 contracts=72 plans=144 receivables=90
}
```

## 迁移约束

- KDL 最终成为唯一真源，不长期维护 YAML/KDL 两份等价配置；
- 迁移期由同一读取层按扩展名选择解析器，业务脚本只调用统一的 `config_get/config_require` 接口；
- 重复节点、未知节点、未知属性、缺失必填项、类型错误和越界值全部快速失败；
- 示例配置必须完整提交，真实账号和秘密只进入本机私有配置；
- 路径继续相对配置文件解析，不相对当前工作目录解析；
- 先迁移生成、编译、镜像构建三个无状态入口，再迁移部署和数据库替换等有状态入口；
- 有状态入口迁移前必须建立 YAML 与 KDL 解析结果等价测试，完成后删除对应 YAML 读取分支和过时文档。

## 阶段

1. 定义 KDL 配置读取器、严格 Schema、错误格式和路径规则；
2. 迁移演示数据生成器并验证生成 SQL、manifest、checksum 与既有契约等价；
3. 迁移 `compile.sh` 和镜像构建入口；
4. 迁移 `deploy.sh`、清理、备份恢复和 BPM provision；
5. 将示例、中文字段说明和 README 切换到 KDL；
6. 删除 YAML 兼容层，执行完整编译、构建、全新卷部署与 replace/preserve 回归。

本轮只确立迁移目标与边界，运行中的数据集替换仍使用现有 YAML 入口，避免在同一次有状态操作中同时更换
配置解析协议和业务数据。
