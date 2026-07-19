# CRM-FEATURE-001：客户查重

## 需求证据

- 修订版：`docs/02-Prototype-Analysis/user-stories/01-Core-Domains.md` 的 `US-CUS-002`。
- 修订版 Gap：`GAP-CUS-001` 要求完善客户唯一性。
- v6 Gap：客户域明确列出“客户查重（名称/手机）”。
- 原型页：`PG-0187 创建客户`；原型本身没有业务提交动作，因此验收规则以修订版 Story 为准。

## 实现边界

- 新增 `GET /admin-api/crm/customer/duplicate-check`，按名称或手机精确匹配，最多返回 20 个候选。
- 普通用户只查询拥有 OWNER/READ/WRITE 对象权限的客户；CRM 管理员可查询全部租户内客户。
- 响应只包含客户 ID、名称和手机，不暴露备注、地址等非必要字段。
- 创建表单提供显式查重按钮，并在提交前自动执行一次查重。
- 同名客户继续沿用既有硬阻断；同手机号只视为疑似重复，用户确认后允许继续创建。
- 用户确认后，创建操作日志追加“已确认疑似重复客户后继续创建”。
- 手机号是否成为硬唯一键仍依赖业务规则签署，本功能不新增唯一索引。

## 代码范围

- CRM Customer Controller、Service、Mapper、请求/响应 VO。
- CustomerForm、客户 API 和中/英/阿三套文案。
- Customer Service 与 Controller 自动化测试。

## 完成证据

- Java 专项 7/7 通过并生成 JaCoCo 报告。
- 前端改动 ESLint 通过，生产构建成功。
- Podman API 返回受控候选；空条件返回 400；排除自身参数生效。
- 普通用户对象权限与跨租户负向矩阵通过，只返回被授权且属于当前租户的候选。
- 使用同手机号创建第二客户成功，操作日志十六进制解码确认带有用户确认文字。
- Web 8081 返回 200，生产资产包含查重文案。
- 两名临时客户、权限和两条操作日志均已清理，残留为 0。

详细用例、结果和覆盖率见 `docs/20-CRM-Delivery/testing/customer-duplicate-check/`。
