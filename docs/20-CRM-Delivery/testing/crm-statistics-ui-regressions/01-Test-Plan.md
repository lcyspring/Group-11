# CRM 统计前端历史回归测试计划

1. 严格 UTF-8 解码销售漏斗 SFC，验证四个 Tab ref 为独立可执行声明；
2. 验证商机汇总请求显式携带本地 `pageNo/pageSize` 且覆盖父级筛选中的同名字段；
3. 验证业绩年份选择模型与 API 起止时间数组分离；
4. 验证代表性统计加载入口使用 `try/finally` 无条件释放 loading；
5. 与既有增长率、目标计算和成交周期纯函数测试一起在 Ubuntu 26.04 容器执行；
6. 执行统计目录 ESLint 和 Web production build。
