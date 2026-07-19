# CRM 对话框异常边界覆盖率

取消原因覆盖 `cancel`、`close`、`confirm`、普通 `Error` 四类；Promise 覆盖成功、用户取消、业务失败
三条分支。源码门禁覆盖 `Web/src/views/crm` 下全部 Vue 文件，当前空 `catch` 为 0。

组合专项 LCOV 的分支、函数和行覆盖率均达到配置阈值；该前端测试不计入 Java JaCoCo。
