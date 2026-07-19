# CRM-NFR-BUG-001：CRM 无受控备份与恢复演练

## 现象

运行环境有持久卷，但没有 CRM 真源备份命令、校验文件、恢复保护或演练证据，无法证明数据卷损坏
后可恢复，也无法给出实际 RPO/RTO。

## 修复

新增 YAML-only MySQL 备份和恢复入口，采用一致性压缩 dump、SHA-256、隔离目标库、七张核心表
验证、演练后清理和真源库二次授权保护。

## 验证

真实备份、checksum 正/负向、369 张表隔离恢复、CRM 核心表 7/7、演练清理和真源双重保护均通过。
见 `docs/20-CRM-Delivery/testing/crm-database-backup-recovery/`。

## 分支

`develop`
