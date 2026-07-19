# WEB-ORB-BUG-002：管理端历史头像触发 OpaqueResponseBlocking

- 模块：管理端 / 用户头像
- 状态：已关闭
- 严重性：P1
- 分支：`develop`

## 现象与根因

登录用户历史头像仍指向退休的 `http://test.yudao.iocoder.cn/user/avatar/**`。管理端此前没有 Mall
已有的遗留媒体保护，API 响应和本地缓存会反复把该跨源地址交给浏览器，产生
`NS_BINDING_ABORTED` 和 `OpaqueResponseBlocking`。

## 修复关键

- Ubuntu 构建 YAML 显式配置退休媒体源并注入 `VITE_APP_LEGACY_MEDIA_ORIGINS`；
- Axios 成功响应递归规范化嵌套 URL；
- 登录用户缓存读取和头像更新再次规范化；
- 退休 URL 转为空值，由现有默认头像接管，不硬编码用户编号或历史文件名。

## 验证

媒体测试 3/3；行覆盖率 96.45%、分支 90.63%、方法 100%；Web production build 成功，已部署
bundle 包含显式退休源，8081 favicon 返回 200。
