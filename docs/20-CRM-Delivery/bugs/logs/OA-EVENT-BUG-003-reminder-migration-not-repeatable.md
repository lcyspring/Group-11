# OA 日程提醒迁移不可重复执行

## 现象

`rebuild-server` 会重新执行 compatibility manifest，日程提醒迁移再次新增已存在字段后失败，导致新 Server 镜像无法热替换，旧服务继续运行。

## 修复

迁移改为读取 `information_schema`，分别判断三个字段和索引，仅在缺失时执行 DDL；通知模板和 Job 继续使用 `NOT EXISTS`。

## 验证

同一数据库连续执行兼容迁移不再因重复字段或索引失败，随后重新部署 Server/Web。
