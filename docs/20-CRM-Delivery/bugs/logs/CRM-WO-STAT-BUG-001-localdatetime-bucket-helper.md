# CRM-WO-STAT-BUG-001：工单趋势使用不存在的 LocalDateTime 工具重载

## 现象

首次 Ubuntu 26.04 CRM 编译在工单统计服务失败，提示
`LocalDateTimeUtils.isBetween(LocalDateTime, LocalDateTime, LocalDateTime)` 不存在。

## 根因

项目公共工具仅提供 `Timestamp` 和字符串版本；现有统计服务通过字符串或数据库时间
类型调用，新增服务误用了未提供的 LocalDateTime 重载。

## 修复

趋势桶聚合改用 `LocalDateTime.isBefore/isAfter` 进行闭区间比较，避免扩展公共工具的
无关变更，同时保留日期边界一致性。

## 验证

修复后重新执行 Ubuntu 26.04 CRM 编译，继续进入测试阶段；专项单测覆盖创建数、完结数
及空日期桶。
