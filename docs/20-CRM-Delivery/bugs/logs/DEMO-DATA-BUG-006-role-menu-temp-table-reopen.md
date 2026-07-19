# DEMO-DATA-BUG-006：角色父菜单递归重复打开临时表

## 现象

首次部署可登录演示组织时，MySQL 在核心插入阶段报错：`Can't reopen table: 'demo_role_menu_ids'`。

## 根因

角色权限先按权限码选中按钮和菜单，再递归补齐父菜单。初版循环在同一条语句中把
`demo_role_menu_ids` 同时作为读取源和写入目标，触发 MySQL 对临时表重复打开的限制。

## 修复与验证

改为 `frontier`、`next`、`all` 三个临时表逐层推进，任一语句不再同时读写同一临时表。重新执行
`deploy.sh` replace 后，5 类角色分别获得 183、68、44、29、48 个菜单节点，加载断言和 Server 启动通过。
