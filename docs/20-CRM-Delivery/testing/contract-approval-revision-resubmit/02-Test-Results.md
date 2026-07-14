# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-ubuntu-26.04.yaml
```

- `CrmContractServiceImplTest`：15/15；
- `CrmAuditStatusUtilsTest`：4/4；
- CRM 全量：124/124；
- Failures：0；Errors：0；Skipped：0；
- Maven reactor：20/20 SUCCESS。

专项覆盖审批中编辑拒绝、驳回修订回草稿、转换来源不可变、新流程重提、WRITE 权限、
取消映射、重复回调、旧流程回调和审批历史删除保护。

## Ubuntu 26.04 Web

- 目标文件 Prettier：通过；
- 目标文件 ESLint：通过；
- CRM 统计纯函数：7/7；
- Vite production build：成功。

## 尚未伪造的运行证据

重复提交由事务行锁串行化、审批终态由条件更新保护，均已完成代码和单元测试。本批未在
已发布的真实 `crm-contract-audit` 流程上执行双请求/API 回调压力测试，因此不把该项写成
真实 Flowable 并发已验证。
