# CRM-CUSTOMER-POOL-BUG-003：领取额度依赖 affected rows 可被误判成功

- 模块：CRM / 客户公海 / 自助领取
- 状态：已修复

旧实现用 `ON DUPLICATE KEY UPDATE IF(...)` 保持超限计数不变，再以 Mapper 返回 0 判断
拒绝。Connector/J 默认可能返回匹配行数而非实际变更行数，导致“计数没增加但业务继续领取”。

修复为条件 UPDATE、INSERT IGNORE、并发首次插入后重试条件 UPDATE 的原子算法，不依赖
驱动 affected-row 兼容开关。五项 Mapper 分支测试通过；真实 MySQL 12 路并发中仅 10 次
成功、2 次拒绝，最终计数严格为 10，临时行已清理。
