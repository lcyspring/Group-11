# CRM-MKT-I18N-BUG-001：CRM 营销新增按钮显示 common.create

## 现象

短信群发和客户关怀的新增按钮直接显示 `common.create`，出现中英文界面混杂。同类问题同时存在于
营销活动和竞品管理页面。

## 根因

四个页面引用了不存在的 `common.create`。项目通用动作词典实际使用 `action.create`，中文、英文和
阿拉伯文均已有该键。

## 修复

把 CRM 营销域四个新增按钮统一改为 `action.create`，不向 `common` 词典加入语义重复的兼容键，
避免继续扩大两套动作命名空间。
全 Web 精确扫描还发现 IoT 设备分组和产品分类两处同类调用，已一并统一，并把门禁扩大到所有 Vue 页面。

## 验证

- 营销域源码 `common.create` 残留 0；
- 短信群发、客户关怀、营销活动、竞品管理 4/4 使用 `action.create`；
- zh-CN/en/ar 三套 `action.create` 词典 3/3 存在；
- 全 Web Vue 页面缺失键精确残留 0；
- Ubuntu 26.04 Web 专项测试和 production build 通过后部署观察。
