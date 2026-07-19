# CRM-FIN-I18N-BUG-001：财务新建弹窗确认和取消显示英文

- 日期：2026-07-16
- 分支：`develop`
- 级别：P2
- 状态：已关闭

## 现象

中文环境打开回款、回款计划、发票、退款或报销新建/编辑弹窗时，底部确认和取消按钮可能显示英文。

## 根因

页面使用了 `dialog.confirm` 和 `dialog.cancel`，但 `dialog` 顶级命名空间只定义弹窗名称、打开和关闭；
确认与取消实际属于 `common` 命名空间。缺失键触发了回退语言。

## 修复关键

- 回款、回款计划、发票、退款、报销和费用分类弹窗统一改用 `common.confirm`、`common.cancel`；
- 发票开具、红冲和作废弹窗的取消按钮同步对齐；
- 增加源码契约，禁止财务弹窗再次引用不存在的 `dialog.confirm/cancel`。

## 验证

- 九个财务弹窗三语键契约：通过；
- 目标目录 ESLint：零错误；
- Ubuntu 26.04 Podman Web production build：通过。
