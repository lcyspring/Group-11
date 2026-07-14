# CRM-CONTRACT-BUILD-BUG-005：Hutool 字符串小写 API 版本不兼容

发现日期：2026-07-14。状态：Fixed。

合同附件 SHA-256 标准化初版使用 `StrUtil.lower`，Ubuntu 26.04 容器编译确认当前项目锁定的
Hutool 版本不提供该方法，CRM 模块编译失败。

修复改用 JDK `String.toLowerCase(Locale.ROOT)`，同时保留空值分支，避免可选 SHA-256 未传时
产生空指针。该转换与 JVM 默认语言无关，适合哈希十六进制值。修复后重新执行 CRM 全量编译测试。
