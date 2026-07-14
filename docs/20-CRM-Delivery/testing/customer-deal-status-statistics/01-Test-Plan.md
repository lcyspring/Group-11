# 测试计划

## 后端

1. 指定负责人时，Service 只把该负责人传入 Mapper；
2. SQL 必须包含逻辑删除、负责人范围、创建时间和成交状态分组；
3. API、Service、VO 和 Mapper XML 在 Ubuntu 26.04 中完成编译；
4. 执行 CRM 全量 `Crm*Test` 并生成 JaCoCo。

## 前端

1. 客户画像页可以编译新增页签和组件；
2. API 类型支持布尔成交状态和客户数量；
3. 图表、列表使用相同数据并计算占比；
4. 在 Ubuntu 26.04 中执行 Vite production build。

## 边界

Mapper 测试锁定 SQL 契约，不等同真实 MySQL 数据对账。四态客户生命周期不在
本次二态统计范围内。
