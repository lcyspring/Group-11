# 测试计划：固定种子演示数据

1. `check` 校验名称、seed、日期、规模上限和输出目录，不写文件；
2. `generate` 生成 insert、cleanup、manifest、SHA-256，且不访问数据库；
3. 一次性 MySQL 卷执行完整 bootstrap、cleanup、核心插入和关联域插入；
4. 核对客户、联系人、线索、跟进、商机、产品、合同、财务、营销、OA、工单等 20 类各 50 条；
5. 核对联系人、合同、财务孤儿引用为 0，商机/合同产品明细各 50 条；
6. 使用 `deploy.sh` 的 `dataset_mode: replace` 替换本机演示批次，随后恢复 `preserve`；
7. 删除一次性验证容器和卷。
