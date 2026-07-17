# CRM 客户导入预检 Bug 与修复日志

日期：2026-07-17。分支：`develop`。

## CRM-CUSTOMER-IMPORT-BUG-001：幂等结果无法反序列化

现象：预检首次确认成功后，再次确认需要读取已保存的导入结果；Jackson 无法创建
`CrmCustomerImportRespVO`，导致幂等路径异常。

根因：响应对象只有 Lombok builder/全参构造语义，没有供 JSON 反序列化使用的默认构造函数。

修复：为响应对象补充无参和全参构造支持，并由 `confirmationPersistsResultAndCanBeRetriedIdempotently`
验证同一预检重复确认返回原结果，不产生第二次客户写入。

## CRM-CUSTOMER-IMPORT-BUG-002：Mockito insert 重载歧义

现象：新增 Service 测试编译时，MyBatis-Plus Mapper 的 `insert` 重载导致 Mockito 参数匹配歧义。

根因：宽泛 `any()` 无法在继承的 Mapper 重载之间确定静态参数类型。

修复：测试使用 `any(CrmCustomerImportPreviewDO.class)` 显式限定类型。CRM Ubuntu 26.04 全量测试
509/509 通过。

## CRM-CUSTOMER-IMPORT-BUG-003：隐藏字典 Sheet 被计入客户行数

现象：真实模板只有 2 行客户，预检却返回“单次不能超过 2000 行”。

根因：模板除第一个客户 Sheet 外还包含数据验证使用的隐藏字典 Sheet；原始读取调用
`doReadAllSync()` 合并了全部 Sheet，隐藏字典行被误当作客户。

修复：原始表头读取固定为第一个业务 Sheet 的 `sheet(0).doReadSync()`，并增加“客户 Sheet + 25 行
参考 Sheet”的回归场景，确保行数、客户名称和最大行数门禁只基于业务 Sheet。

## CRM-CUSTOMER-IMPORT-BUG-004：预检遗漏手机号重复

现象：模板中的两个不同客户使用同一手机号时，预检仍把两行都标记为可创建。

根因：预检只按客户名称查重，没有复用客户创建界面的“名称或手机号”重复语义。

修复：一次批量加载文件涉及的同名/同手机号客户，分别建立名称和手机号索引；文件内手机号重复及手机号
已归属其他客户都落到逐行错误。批量查询替代逐行查询，最大 2000 行时不会产生 N+1 数据库访问。
