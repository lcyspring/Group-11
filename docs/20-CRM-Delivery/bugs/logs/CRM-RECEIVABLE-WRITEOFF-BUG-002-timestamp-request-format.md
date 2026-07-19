# CRM-RECEIVABLE-WRITEOFF-BUG-002：回款核销时间参数类型错误

- 发现日期：2026-07-19
- 分支：`develop`
- 状态：Closed

## 现象

在回款管理中对已审核回款执行“回款核销”并提交时，接口返回请求参数类型错误，核销记录无法创建。

## 根因

前端日期控件发送 `YYYY-MM-DD HH:mm:ss` 格式的文本；CRM 后端统一的
`TimestampLocalDateTimeDeserializer` 仅接受 epoch 毫秒时间戳或数字字符串，导致
`writeOffTime` 无法反序列化为 `LocalDateTime`。

## 修复

- 回款核销日期控件改用 `value-format="x"`，直接提交 epoch 毫秒值；
- 打开核销弹窗时使用 `Date.now()` 初始化时间；
- 未修改后端 DTO，保持项目统一的时间参数契约。

## 回归结果

- `deno test -A --no-check src/views/crm/receivable/receivableWriteOff.test.mjs`：1/1 通过；
- 测试确认请求使用数字时间戳，并禁止旧的格式化日期字符串；
- Web 镜像重新部署后，真实请求使用 epoch 毫秒时间戳，不再触发参数类型错误。
