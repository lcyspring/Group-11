# BPM-APPROVAL-I18N-BUG-004 审批意见标签泄漏国际化键

## 现象

点击审批通过后，意见框标签显示 `bpm.approval.approval意见`，占位符也混入未翻译的键名。

## 根因

通用审批组件读取 `approval.approval` 作为节点名称，但三语言 BPM 语言包只定义了 `approvalNode`，
没有定义组件实际使用的 Key；插值因此把原始键名拼进了中文“意见”。

## 修复

- 在中、英、阿 BPM 语言包补充 `approval.approval`；
- 保留 `approval.opinionLabel` 和 `approval.opinionPlaceholder` 的统一插值逻辑；
- 增加三语言专项契约测试，防止再次显示裸 Key。
