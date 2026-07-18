# CRM 日期入参契约加固

- 分支：`develop`
- 状态：完成

CRM 表单中的 `LocalDateTime` JSON 字段统一接受毫秒时间戳数字，以及 Element Plus
`value-format="x"` 产生的毫秒数字字符串。非数字文本、布尔值等非契约值直接返回参数
类型错误，不再静默转换为 Unix 起点。

该保护在公共 Jackson 反序列化边界实现，因此同时覆盖回款、回款计划、合同、
商机等使用同一日期契约的写入入口，避免业务层重复校验和遗漏。

构建与专项测试只通过显式 YAML 调用：

```bash
bash podman/compile.sh podman/config/verify-framework-datetime-ubuntu-26.04.yaml
```
