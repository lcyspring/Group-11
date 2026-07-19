# CRM 文件、附件和导出权限测试计划

## CRM 自动化

- 管理员导出不受对象级二次门禁影响；
- OWNER、WRITE 和下属 OWNER/WRITE 可以导出；
- READ-only 协作者和无范围对象必须拒绝，混合 ID 整批失败；
- 合同附件上传使用 YAML 目录并要求合同 WRITE；
- 创建附件拒绝非受管 URL和受管但非保护目录文件；
- 下载校验合同存在、附件归属和合同 READ，并读取受管文件内容；
- 生命周期响应不得暴露 `fileUrl`。

## Infra 自动化

- 公共文件路由命中受保护路径时返回 404，且不调用底层存储；
- 前导斜线、反斜线和 `public/../crm-protected` 不能绕过；
- 相邻目录 `crm-protected-public` 不应被误判；
- `File*Test` 全量回归并生成独立 JaCoCo 报告。

## 前端与构建

- 合同附件 API、面板和下载工具执行零警告专项 ESLint；
- Server、CRM/Infra 测试、覆盖率和 Web production build 全部在 Ubuntu 26.04 Podman 容器执行；
- 缓存构建镜像不得导致新增 YAML 测试开关静默失效。

## 运行验收

- 新附件上传后 `infra_file.path` 位于 `crm-protected/contract/{contractId}/`；
- 公共 `/infra/file/{configId}/get/**` 返回 404；
- 合同授权下载返回原始内容，无 Token 和无对象权限请求被拒绝；
- 管理员导出成功，只读协作者导出返回对象权限错误；
- 历史工单站内信恢复正确中文且双重编码特征为 0。
