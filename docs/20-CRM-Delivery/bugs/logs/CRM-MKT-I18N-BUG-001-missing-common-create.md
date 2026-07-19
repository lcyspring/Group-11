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

## 运行态复验

2026-07-17 再次收到短信群发和客户关怀显示 `common.create` 的反馈。源码检查没有发现回归，
但运行态必须独立于源码重新验收，因此重新执行 Ubuntu 26.04 专项测试和 production build，
并使用 `runtime-local-rebuild-web.yaml` 替换 Web 容器。

- 专项测试：1/1，通过；
- production build：通过；
- 编译产物中短信群发和客户关怀页面块均调用 `action.create`；
- 编译产物精确 `common.create` 残留 0；
- 工作区与运行容器 `index.html` SHA-256 均为
  `bfd5279d84f94ec1e2b800a2995e9232637fc51c9f0efb759bf78a6bc6e08ad6`。

因此本次复验排除了源码和容器产物回归。若已打开的浏览器标签仍显示旧键，需要强制刷新一次，
使其丢弃此前加载的入口和动态页面块；刷新后继续出现则应记录当前页面 URL 和入口资源哈希，
不能再用浏览器旧资源替代运行容器证据。
