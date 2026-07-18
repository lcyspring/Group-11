# 合同补充协议测试计划

执行环境：rootless Podman、Ubuntu 26.04、OpenJDK 17、Maven 3.9、Node 22、pnpm 11。

## 自动化范围

1. 创建资格、请求幂等及冲突请求；
2. 合同与产品前后快照、金额差额、产品行归属及重复行保护；
3. 已有税率继承、折扣后税额及含税金额重算；
4. 已审批回款、回款计划和净开票金额下限；
5. 依据附件绑定、删除边界、提交前必填和审批生效锁定；
6. 独立 BPM 提交、重复/过期回调、条件终态竞争和事务回滚；
7. 草稿候选版本不推进当前有效版本；
8. Controller 权限、嵌套请求校验和响应快照隔离；
9. 前端创建、编辑、产品行、附件、提交、状态展示及三语资源。

## 真实运行验收

使用 `podman/tests/acceptance/verify-crm-contract-amendment.sh`，命令行只传 KDL 路径。验收应覆盖：

- 从已审批且实际签署合同创建补充协议；
- 上传受保护依据附件并提交 `crm-contract-amendment-audit`；
- 审批通过后合同金额、产品和版本投影生效；
- 依据附件变为不可变，生命周期出现创建、提交和生效动作；
- 跨合同读取和跨租户读取拒绝；
- 重复迁移与重复流程治理保持幂等。

## 构建门禁

- CRM 全量自动化和 JaCoCo；
- 补充协议前端专项 ESLint；
- Ubuntu 26.04 Server package；
- Ubuntu 26.04 Web production build；
- `git diff --check` 和 KDL/Shell 语法检查。
