# OA 日程新增按钮语言不一致

## 现象

日程新增/修改按钮使用不存在的 `common.create`、`common.update`，导致显示英文或裸语言键。

## 修复

改用日程域自己的 `oa.event.create/update`，补齐中文、英文和阿拉伯文文案及回归测试。
