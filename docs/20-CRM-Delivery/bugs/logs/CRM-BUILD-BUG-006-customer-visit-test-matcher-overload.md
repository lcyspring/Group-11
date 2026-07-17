# CRM-BUILD-BUG-006：客户拜访测试触发 Mapper 重载歧义

日期：2026-07-17  
分支：`develop`

## 现象

其他 Host 使用统一 Ubuntu 26.04 构建流程时，`mitedtsm-module-crm` 在 Maven `testCompile` 阶段失败，
外层只显示 Surefire/Maven 模块失败。

## 根因

MyBatis-Plus 3.5.15 的 `BaseMapper` 同时提供单对象与集合版本的 `insert/updateById`。客户拜访测试使用
未限定类型的 Mockito `argThat/any`，Java 编译器无法判断目标重载。该问题属于共享源码测试编译错误，
与 Host 的 JDK、Maven 或 Podman 环境无关。

## 修复

- 对 `insert` 使用 `any(CrmCustomerVisitDO.class)`；
- 对 `updateById` 使用显式泛型 `ArgumentMatchers.<CrmCustomerVisitDO>argThat(...)`；
- 继续使用公共 `ghcr.io/elel-code/group-11-build-ubuntu:26.04` 验证，不增加 Host 工具链兼容分支。

## 验证

- CRM 490/490，失败 0、错误 0、跳过 0；
- 客户拜访专项 4/4；
- Web 3/3，专项覆盖率 100%；
- production Web 构建成功。
