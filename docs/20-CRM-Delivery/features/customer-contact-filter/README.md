# CRM-FEATURE-004：客户列表按联系人筛选

## 需求证据

- `docs/Proj-Docs-v-6/05-Business-Requirements/02-SRS-User-Stories.md` 的 `CUS-001`：客户列表搜索栏包含“联系人”和“首联系人”。
- `docs/03-Gap-Analysis/03-Business-Domain-Gaps.md` 的 `GAP-CUS-001`：客户列表字段和筛选需与原型逐项映射。
- `docs/02-Prototype-Analysis/user-stories/01-Core-Domains.md` 的 `US-CUS-001`：客户列表分页、筛选和授权范围应一致。

## 实现范围

- 客户分页请求新增 `contactName` 和 `primaryContactName`。
- `contactName` 对客户下任意有效联系人姓名做模糊匹配。
- `primaryContactName` 只对 `primary_contact=true` 的有效联系人姓名做模糊匹配。
- 两个条件使用独立联系人 JOIN，可表达“客户有联系人 A，同时首联系人是 B”。
- 一对多 JOIN 使用 `DISTINCT`，避免同一客户多个联系人命中时列表和分页总数重复。
- 查询继续叠加原有 OWNER/INVOLVED/SUBORDINATE/公海范围，不绕过对象权限。
- Web 搜索区增加两个输入框，并同步中、英、阿三语文案。

## 完成证据

- Mapper 查询结构测试 3/3，CRM 全量 36/36。
- 真实 MySQL/API 验证 5/5：模糊匹配、分页去重、首联系人限定、独立组合、负责人权限和空结果均符合预期。
- 前端 ESLint、Web 生产构建、Server 生产 JAR、rootless Podman 重建通过。
- 部署资产已检出三语首联系人筛选文案。
- 临时客户、联系人、日志、权限均清理为 0。

详细测试与覆盖率见 `docs/20-CRM-Delivery/testing/customer-contact-filter/`。
