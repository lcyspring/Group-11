# CRM-FEATURE-003：联系人手机号必填及客户内唯一

## 需求证据

- `docs/Proj-Docs-v-6/05-Business-Requirements/02-SRS-User-Stories.md` 的 CUS-009：手机号必填；同一客户与同一手机号已存在时拒绝保存；手机号与客户 ID 唯一。
- `docs/03-Gap-Analysis/03-Business-Domain-Gaps.md` 的 `GAP-CUS-002`：联系人编辑边界需要完善。
- 与 `CRM-FEATURE-002` 共用客户行锁，以同一客户为一致性边界。

## 实现规则

- 创建和更新 API 使用 `@NotBlank` 与已有 `@Mobile`，分别校验必填和格式。
- Web 联系人表单在提交前执行必填校验，中、英、阿文案保持一致。
- 服务层先去除手机号两端空格，再在客户行锁内执行重复检查。
- 重复查询使用 `SELECT ... FOR UPDATE` 当前读，避免 MySQL RR 旧快照漏掉刚提交的联系人。
- 更新时排除当前联系人 ID；同一手机号允许出现在不同客户。
- 冲突返回业务错误 `1020003008`。

## 数据库约束

迁移 `database/migrations/new-crm-contact-mobile-unique.sql` 增加：

- `active_mobile_key` 存储生成列；只为 `deleted=0`、客户和手机号均有效的联系人生成租户级组合键。
- 唯一索引 `uk_crm_contact_active_mobile`。
- 查询索引 `idx_crm_contact_customer_mobile`，避免客户内查重退化为联系人全表扫描。
- 逻辑删除记录的生成键为 `NULL`，因此号码可重新使用，也不会限制多条历史删除记录。
- 迁移幂等，可重复执行。

现有环境盘点为 9 条有效联系人，其中 5 条历史记录手机号为空、非空手机号重复组为 0。迁移不伪造历史手机号，也不把列强改为 `NOT NULL`；新建和更新从 API 边界开始强制必填。

## 完成证据

- 联系人专项测试 12/12、CRM `Crm*Test` 29/29。
- Podman 真实 API/MySQL 验证 7/7，包括并发创建与逻辑删除后复用。
- SQL 连续执行两次成功，生成列和唯一索引各一项。
- ESLint、Server 生产 JAR、Web `build:prod` 均通过。
- 临时客户、联系人、操作日志和权限记录清理后均为 0。

详细计划、结果和覆盖率见 `docs/20-CRM-Delivery/testing/contact-mobile-unique/`。
