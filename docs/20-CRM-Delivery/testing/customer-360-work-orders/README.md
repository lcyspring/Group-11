# 客户 360 工单记录测试

## 覆盖点

- 客户 ID 作为分页筛选条件；
- 分页总数和列表更新；
- 工单详情轨迹弹窗；
- 默认个人范围和 `query-all` 范围不被客户详情绕过；
- 三语 Tab 与字段文案；
- Ubuntu 26.04 Web 生产构建。

## 覆盖率说明

本批为 Vue 组合与既有后端权限接口复用，不新增 Java 可执行行；JaCoCo 不变化。使用
Ubuntu 26.04 Vite 构建与真实客户/工单接口作为验收证据。

## 最终结果

- Ubuntu 26.04 Web 生产构建：通过；
- 热替换后的 Web 首页：`200 OK`；
- `/admin-api/crm/work-order/page?pageNo=1&pageSize=10&customerId=17`：`code=0`；
- 客户 17 返回 2 条工单，且响应中的 `customerId` 均为 17；
- Server、Web、Mall 同时可用，验收期间未出现持续 502。
