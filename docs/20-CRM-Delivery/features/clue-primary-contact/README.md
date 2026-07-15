# CRM-FEATURE-005：线索转客户创建首联系人

## 需求证据

- `docs/02-Prototype-Analysis/user-stories/01-Core-Domains.md` 的 `US-LEAD-008`：线索转客户时迁移首联系人和历史上下文，且全部成功或保持原状。
- `docs/03-Gap-Analysis/03-Business-Domain-Gaps.md` 的 `GAP-LEAD-002`：现有转换缺少首联系人、完整活动迁移清单、幂等和失败回滚规则。
- `docs/03-Gap-Analysis/02-End-to-End-Traceability-Matrix.md`：要求重复请求幂等、部分失败回滚并保留来源/目标关系。
- `docs/Proj-Docs-v-6/05-Business-Requirements/02-SRS-User-Stories.md`：客户只允许一个首联系人。

## 本阶段实现

- 转换弹窗要求显式填写首联系人姓名和手机号，手机号默认带出线索手机号；不把线索名称伪装为联系人姓名。
- 转换 API 改为 JSON 请求体：`id`、`contactName`、`contactMobile`，三个字段均在后端校验。
- 同一事务中执行：原子抢占转换权、创建客户、创建首联系人、回写线索客户 ID、复制已有跟进记录。
- 首联系人继承线索电话、微信、邮箱、地区和详细地址，负责人为执行转换的线索负责人/登录用户。
- 联系人创建失败时，客户、转换状态和线索客户 ID 全部回滚。
- 中、英、阿三语转换弹窗文案同步。

## 并发与一致性

转换前用 `transform_status=false` 作为更新条件原子抢占。只有影响一行的请求可以继续创建客户；重复或并发失败请求返回 `1020001001`。客户、联系人、线索回写及跟进复制均受同一个 Spring 事务保护。

真实 MySQL 测试通过短时安装受控联系人插入失败触发器，验证异常后线索仍为未转换、`customer_id` 为空，且客户和联系人均未留下，随后立即移除触发器。

## 当前边界

本阶段完成 `GAP-LEAD-002` 的首联系人、同一线索重复/并发保护和失败回滚，并继续迁移系统原有
的跟进记录。后续 `CRM-FEATURE-043` 已补齐任务、通话、短信真源、同事务迁移和唯一审计，
两项功能共同关闭该差异。

## 完成证据

- 请求校验测试 3/3、转换 Service 测试 2/2、CRM 全量 39/39。
- Rootless Podman 真实转换 7/7，包括重复、并发和受控故障回滚。
- 首联系人生命周期、手机号唯一性、锁等待当前读回归共 18/18。
- ESLint、Web `build:prod`、Server 生产 JAR、Podman Server/Web/Mall 健康检查均通过。
- 测试线索、客户、联系人、操作日志、权限和故障触发器清理后均为 0。

详细测试与覆盖率见 `docs/20-CRM-Delivery/testing/clue-primary-contact/`。
