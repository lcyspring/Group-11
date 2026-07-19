# DEMO-DATA-BUG-001：生成数据 cleanup 批次匹配排序规则冲突

日期：2026-07-18。分支：`develop`。状态：已修复。

## 现象

第一版固定种子数据在临时 MySQL 8 数据库成功生成，但执行 cleanup 时，批次前缀 `LIKE` 返回
`Illegal mix of collations`，生成表列为 `utf8mb4_unicode_ci`，会话变量采用 MySQL 8 默认
`utf8mb4_0900_ai_ci`。

## 修复

insert 和 cleanup 模板中的 `@demo_batch` 均使用 `_utf8mb4` 字面量并显式指定
`utf8mb4_unicode_ci`。生成器仍只渲染文件，不连接数据库。

## 回归要求

在隔离库重新生成数据后，验证数量、孤儿引用、客户生命周期、工单状态，再执行 cleanup 并确认六张
生成表相关批次残留均为 0；随后删除临时数据库。
