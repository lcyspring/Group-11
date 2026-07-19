# CRM-RECEIVABLE-I18N-BUG-004 回款计划合同编号占位符显示语言 Key

## 现象

回款计划的合同编号筛选框显示 `crm.receivablePlan.contractNoPlaceholder`，没有显示当前语言文案。

## 根因

页面已引用专用占位符 Key，但中、英、阿三份 CRM 语言包均未定义该字段。

## 修复

补齐三语言 `receivablePlan.contractNoPlaceholder`，保留统一的 `t()` 调用，不在页面硬编码中文。

## 验证

中文显示“请输入合同编号”，英文和阿拉伯文切换后显示对应翻译，页面不再出现裸 Key。
