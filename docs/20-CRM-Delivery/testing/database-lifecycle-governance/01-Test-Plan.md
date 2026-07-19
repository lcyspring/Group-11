# 数据库脚本生命周期测试计划

1. Shell 语法检查 MySQL 初始化脚本；
2. 校验 bootstrap 与 compatibility manifest 的所有相对路径存在且不越界；
3. 校验清理和销毁目录未进入自动执行清单；
4. 校验 compatibility 是 bootstrap 的子集；
5. 校验全部 CRM 增量迁移均已登记；
6. 校验 seed 不含建表、更新、删除、清空或销毁语句；
7. 使用空临时 MySQL 数据卷执行完整 bootstrap，并检查关键表；
8. 对运行库执行兼容迁移并确认现有 8080/8081/8082 服务可恢复。
