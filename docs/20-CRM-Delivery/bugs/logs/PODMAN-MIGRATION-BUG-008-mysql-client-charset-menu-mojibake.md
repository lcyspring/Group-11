# PODMAN-MIGRATION-BUG-008：MySQL 客户端字符集导致 CRM 菜单双重编码

- 发现/关闭日期：2026-07-15
- 级别：P1 / 部署与数据一致性

## 现象

CRM 侧栏新增菜单显示为 `å‘ç¥¨ç®¡ç†`、`å®¢æœå·¥å•` 等乱码。受影响项包括工单、
工单统计、发票、合同附件/签署和退款/冲销菜单，也包括相应权限名称、字典标签、通知模板和三语菜单。

数据库字节核验确认并非浏览器字体：例如正确 `发票管理` 应以 `E58F91...` 开头，问题数据为
`C3A5C28F...`，属于 UTF-8 字节按 latin1 解码后再次编码。

## 根因

首次初始化脚本和运行期兼容迁移都直接调用 MySQL CLI，没有设置
`--default-character-set=utf8mb4`。服务端表字符集为 utf8mb4 并不能修正客户端连接协商错误，
含中文的 INSERT 因而写入双重编码文本。

## 修复关键

- `podman/up.sh` 的迁移、运行配置更新和 schema 探针统一使用 YAML 中的 `mysql.character_set`；
- `podman/init/init-mysql.sh` 通过单一 `mysql_utf8` 入口执行所有初始化 SQL；
- 新增幂等 `repair-crm-menu-utf8.sql`，按稳定 component、permission、dict type/value 和通知 code 修复，
  不依赖乱码文本匹配；
- 受影响源迁移在正确连接字符集下重新执行，以 upsert 恢复中、英、阿三语记录；
- 修复迁移加入 YAML 指定的 compatibility manifest，持久卷和新建环境走同一路径。

## 验证

- 两个 Shell 脚本 `bash -n` 通过；
- 六个受影响迁移按正确字符集重复执行均成功；
- ID 6000+ 菜单双重编码特征扫描从 26 项受影响降为 0；
- `发票管理` 恢复为 `E58F91E7A5A8E7AEA1E79086`，退款/冲销、工单、合同菜单均为正确 UTF-8；
- 字典、工单通知和三语 `system_menu_i18n` 扫描无 `C383` 双重编码特征；
- Ubuntu 26.04 Server/Web 构建通过，CRM 224/224。

## 清理与遗留风险

本次修复覆盖应用可见的菜单、权限、字典、通知和三语数据。历史 DDL 的中文 COMMENT 只影响数据库
元数据展示，不参与业务读取；后续若需要统一数据字典，可单独生成 COMMENT 修复迁移，避免在本次功能
迁移中批量重定义列类型。
