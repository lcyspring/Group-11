# 数据集集中模式测试计划

1. `preserve` 对已有库不执行数据集；
2. `insert` 拒绝包含 cleanup 的 manifest；
3. `replace` 拒绝缺少首项 cleanup 的 manifest；
4. 合法 replace 严格按 cleanup→insert 顺序执行；
5. 空库仍执行 bootstrap 和选定数据集；
6. 未识别的非空库拒绝破坏性初始化；
7. check 模式不改变 Pod、镜像、容器或卷。
