# CRM-CUSTOMER-TEST-BUG-001：画像测试仍构造旧二态字段

发现日期：2026-07-14。状态：Fixed。分支：`develop`。

客户画像响应从布尔 `dealStatus` 升级为四态 `lifecycleStatus` 后，画像 Service 既有测试仍调用
`setDealStatus(boolean)`，导致 Ubuntu 26.04 容器首次测试编译失败。主代码已编译，问题位于测试
数据构造与新响应契约不同步。

修复为按 10、20、30、40 构造四条状态样例，并继续验证选定负责人范围被透传到 Mapper。修复后
CRM 全量 204/204，Server 和 Web 生产构建均通过。本日志保留首次失败和契约修复事实，避免把
测试编译失败误报为功能通过。
