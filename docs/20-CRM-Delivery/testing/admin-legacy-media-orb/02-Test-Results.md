# 管理端遗留媒体 ORB 测试结果

- 日期：2026-07-16
- 状态：通过

| 检查项 | 结果 |
|---|---|
| Node 自动化 | 3/3，失败 0 |
| TypeScript/配置专项 ESLint | 0 warning、0 error |
| 行覆盖率 | 96.45% |
| 分支覆盖率 | 90.63% |
| 方法覆盖率 | 100% |
| Ubuntu 26.04 Web build | 成功 |
| 已部署 bundle | 包含 YAML 注入的 `test.yudao.iocoder.cn` 退休源 |
| 8081 favicon | HTTP 200 |

数据库仍保留历史头像 URL 作为原始数据；浏览器响应层和用户缓存层会将该退休媒体 URL 转为空，
由现有默认头像组件接管，不再向旧域名发起图片请求。
