# CRM 客服工单最小闭环测试

## 自动化

执行入口：

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-ubuntu-26.04.yaml
```

环境：Ubuntu 26.04、OpenJDK 17.0.19、Maven 3.9.12。

结果：

- CRM 全量：161/161，通过 161，失败 0，错误 0，跳过 0；
- 工单专属：6/6；
- 覆盖创建初始轨迹和通知、处理人权限、原子开始处理、完结方案必填、
  退回原因和通知、来源客户不一致。

## JaCoCo

| 范围 | 指令 | 分支 | 行 | 方法 |
| --- | ---: | ---: | ---: | ---: |
| CRM 模块整体 | 34.04% | 33.10% | 32.07% | 22.39% |
| `CrmWorkOrderServiceImpl` | 53.08% | 42.55% | 57.41% | 62.50% |

原始报告：`Server/mitedtsm-module-crm/target/site/jacoco/`（构建产物，不提交）。

## 构建与运行

- Web Ubuntu 26.04 生产构建：通过；
- Server Ubuntu 26.04 打包：通过；
- SQL 连续执行两次：通过，权限 6 条、通知模板 3 条，无重复；
- 真实 API：创建、开始、完结、详情轨迹、待办退出均通过；
- 站内信：分派与完结消息落库通过。
