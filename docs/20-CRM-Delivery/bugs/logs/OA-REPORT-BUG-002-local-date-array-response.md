# OA-REPORT-BUG-002 工作报告日期响应为数组

## 现象

真实 API 返回的报告日期和周期是 `[2026,7,18]`，前端字段契约为 `YYYY-MM-DD`，列表和详情无法稳定展示。

## 根因

工作报告响应 VO 直接暴露 `LocalDate`，当前 Jackson 全局设置将其序列化为数组；响应字段没有声明业务日期格式。

## 修复

对 `reportDate`、`periodStart` 和 `periodEnd` 显式配置 `yyyy-MM-dd` JSON 格式，避免依赖全局序列化策略。

## 验证

真实 API 创建并读取日报后，三个字段均返回 ISO 日期字符串；提交后修改继续被状态机拒绝。
