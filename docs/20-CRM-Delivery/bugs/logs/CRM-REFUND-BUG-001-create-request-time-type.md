# CRM-REFUND-BUG-001：退款创建请求时间参数类型错误

- 发现日期：2026-07-19
- 分支：`develop`
- 状态：Closed

## 现象

在退款管理中新建退款/冲销草稿，点击“确认”后接口返回请求参数类型错误，草稿无法创建。

## 根因

退款表单将 `refundTime` 发送为 `YYYY-MM-DD HH:mm:ss` 格式文本；后端字段为
`LocalDateTime`，项目统一的 JSON 时间契约要求 epoch 毫秒时间戳，字符串无法完成反序列化。

## 修复

- 日期控件改为输出 epoch 毫秒值，并使用 `Date.now()` 初始化新建时间；
- 提交前校验并规范化时间戳，创建与编辑采用相同契约；
- 新增独立保存命令类型，只提交服务端保存接口接受的字段，避免详情响应字段混入请求。

## 回归结果

- `deno test -A --no-check Web/src/views/crm/refund/constants.test.mjs`：2/2 通过；
- `deno task lint:crm-refund`：通过，零 ESLint warning；
- 测试覆盖创建与编辑命令的 epoch 毫秒契约，并禁止直接提交详情响应对象。
