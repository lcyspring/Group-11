# 测试计划

- Service：未审批合同、计划总额上限、审批通过正向、关联计划删除、并发重复关联；
- Mapper：未审批回款仍为待回款、已回款只匹配审批通过、角标与列表日期边界一致；
- Controller：待回款、逾期、已回款三种状态及已收/未收金额；
- 回归：CRM 全量、Prettier、ESLint、Web production build；
- 环境：Podman 中 Ubuntu 26.04 专用构建镜像。
