# 数据库初始化数据集测试结果

日期：2026-07-17。分支：`develop`。

- `none` 独立空卷：CRM 核心业务记录 0、CRM 表 67、用户 1、租户 1、必要部门链 3、管理员角色 `1,2`；
- `legacy-demo-v1` 独立空卷：用户 20、CRM 核心演示记录 74；
- 两个临时数据库容器、卷和测试镜像已清理；
- 数据集路径越界、teardown 引用、缺失 manifest 均受门禁约束；
- `cleanup_existing_before_dataset=false` 时执行 cleanup 数据集被拒绝；
- `replace` 未开启 `confirm_persistent_data_change` 时被拒绝；
- 现有运行 Pod 未因数据集检查发生变化。
