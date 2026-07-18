# 权限与运行安全验收

## CRM 对象权限

对同一客户及其联系人、商机、合同、回款、退款、发票、报销、附件、工单和导出，分别使用负责人、
协作人、同部门非成员、跨部门和另一租户账号验证。页面隐藏按钮不是充分证据，必须直接调用对应
API 做负向检查。

预期：OWNER 可写，READ/WRITE 成员按授权等级操作；部门范围与对象范围取允许集合，不因查询接口
遗漏注解而扩大；公共池对象只允许显式领取/分配命令改变归属。

## 资源与部署安全

- 合同及 CRM 附件只能通过受保护路由下载，猜测公共 URL 不得成功；
- 八类导出复用对象权限和租户条件；
- `http://127.0.0.1:8080/actuator/env`、OpenAPI、Swagger、Druid 不暴露真实内容；
- 历史 `Bearer test1` Mock Token 必须被拒绝；
- 只允许运行 YAML 配置的 `8081` Origin，非可信 Origin 不返回 allow-origin；
- BCrypt 强度 10～16，Mock、文档、Druid 和管理端点均由 YAML 显式控制。

自动验收命令：

```bash
bash ./podman/tests/acceptance/verify-crm-runtime-security.sh ./podman/config/runtime-local.yaml
```
