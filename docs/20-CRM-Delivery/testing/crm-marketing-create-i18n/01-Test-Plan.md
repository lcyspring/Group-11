# CRM 营销新增按钮国际化测试计划

1. 扫描短信群发、客户关怀、营销活动、竞品管理四个页面；
2. 拒绝任何 `common.create` 调用；
3. 要求四页均使用共享 `action.create`；
4. 检查 zh-CN、en、ar 三套动作词典均定义 `create`；
5. 递归扫描全 Web Vue 页面，拒绝任何精确 `common.create` 调用；
6. 在 Ubuntu 26.04 Web 构建容器执行专项测试和 production build。
