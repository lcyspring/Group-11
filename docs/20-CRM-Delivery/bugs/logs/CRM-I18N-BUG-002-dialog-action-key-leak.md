# CRM-I18N-BUG-002：CRM 弹窗操作按钮显示语言键

- 日期：2026-07-16
- 分支：`develop`
- 级别：P1 / 交互一致性
- 状态：已关闭

## 现象

线索编辑弹窗的确认、取消按钮显示为 `dialog.confirm` 和 `dialog.cancel`，用户无法看到正常中文操作文案。

## 根因

组件使用 CRM 局部多语言命名空间，但按钮引用了不存在的 `dialog` 键。多语言库找不到翻译后直接回显键名。同一写法还存在于线索转换、公海分配、工单、产品、权限和跟进弹窗。

## 修复关键

- 统一改用三语均已定义的 `common.confirm`、`common.cancel`；
- 治理 CRM 树内全部同类残留，不局限于线索编辑；
- 增加 CRM 全量 Vue 文件扫描门禁。

## 验证

- CRM Vue 文件扫描：163/163；
- `dialog.confirm/cancel` 残留：0；
- 专项自动化：1/1；
- Ubuntu 26.04 Web production build：通过。
