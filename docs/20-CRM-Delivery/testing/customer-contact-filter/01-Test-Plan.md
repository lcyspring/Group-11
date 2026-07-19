# 客户联系人筛选测试计划

## 目标

验证客户列表的任意联系人和首联系人筛选语义、分页去重、组合条件、数据权限及前端交付资产。

## 自动化

1. 无联系人条件时不产生额外联系人 JOIN，也不启用 DISTINCT。
2. 任意联系人条件使用 `contact_filter` 别名和 LIKE，并启用 DISTINCT。
3. 两个条件同时存在时使用两个独立别名；首联系人 JOIN 包含 `primary_contact=true`。
4. 执行 CRM `Crm*Test` 全量回归并生成 JaCoCo。

## 真实环境

使用 rootless Podman 的 Server/MySQL/Web：

1. 同一客户两个联系人同时命中“采购”，响应只返回一个客户且 `total=1`。
2. 普通联系人和另一客户首联系人包含不同姓名片段，首联系人筛选只命中首联系人。
3. 任意联系人和首联系人组合可由同一客户的不同联系人分别满足。
4. 其他负责人客户即使联系人命中，在 OWNER 场景仍不可见。
5. 不存在姓名返回空分页。
6. 验证生产构建和部署资产三语文案。

临时真实验证脚本：`/tmp/crm-customer-contact-filter-integration.sh`，不提交 Git。
