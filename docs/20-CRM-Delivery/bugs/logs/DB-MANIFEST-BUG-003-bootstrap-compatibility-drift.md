# DB-MANIFEST-BUG-003：空库 Bootstrap 遗漏三项兼容迁移

## 发现

完整 KDL 配置门禁比较 `mysql-bootstrap.manifest` 与 `mysql-compatibility.manifest` 时，发现工单导出、
营销收件人名称快照和 BPM 自定义详情路径修复只存在于升级清单。

## 根因

新增幂等迁移时只更新了已有库兼容清单，没有同步空库 bootstrap 清单。

## 影响

全新数据库卷不会得到对应表字段、菜单和流程详情路径修复，导致新环境与从旧版本升级的环境结构不一致。

## 修复

按兼容清单中的相同相对顺序，将以下文件加入空库 bootstrap：

- `new-crm-work-order-export.sql`；
- `new-crm-marketing-recipient-name-snapshot.sql`；
- `repair-bpm-custom-view-component-paths.sql`。

## 回归

重新执行配置门禁的 bootstrap/compatibility 差集检查，差集为空；随后继续执行完整 KDL 配置测试。
