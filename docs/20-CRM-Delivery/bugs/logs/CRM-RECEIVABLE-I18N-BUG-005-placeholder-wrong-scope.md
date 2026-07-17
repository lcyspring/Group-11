# CRM-RECEIVABLE-I18N-BUG-005 合同编号占位符加入错误语言作用域

## 现象

补充翻译并重新部署后，回款计划仍显示 `crm.receivablePlan.contractNoPlaceholder`。

## 根因

语言文件中存在多个 `contractNo` 字段，首次修复把新 Key 加到了相邻的回款对象，而页面读取的是
`receivablePlan` 对象。Key 存在但作用域错误。

## 修复

- 将三语言占位符移动到 `receivablePlan` 对象；
- 在回款计划目录增加专项测试，校验页面引用以及中、英、阿三份 Key 均位于目标对象，不再只检查全文件是否出现字符串；
- 重新构建并部署 Web 产物。
