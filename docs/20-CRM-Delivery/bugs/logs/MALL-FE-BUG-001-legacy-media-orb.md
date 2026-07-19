# MALL-FE-BUG-001：旧演示媒体域名触发 ORB

## 现象

本机 Mall H5 控制台连续报告 `OpaqueResponseBlocking`，第一批涉及装修横幅、菜单
图标、优惠券背景及两个商品哈希图片；首次修复后，分类页又暴露 9 个分类哈希图
片。浏览器同时请求不存在的 `/favicon.ico`。

## 根因

- 当前装修 API 仍返回 `http://test.yudao.iocoder.cn/static/**`；
- 该旧文件域名在当前环境不可连接，而对应 `static/**` 文件已经随 H5 部署；
- 两个商品哈希 URL 同样指向旧域名，但初始化库没有对应 `infra_file` 记录，仓库也
  没有原始文件；
- `sheep.$url.cdn()` 将完整 URL 原样返回，浏览器收到跨源错误/非图片响应后由 ORB
  阻止渲染。
- 分类页 `second-one.vue` 直接绑定 `item.picUrl`，绕过了 `cdn()`，证明仅修 URL
  展示工具不能覆盖全部业务响应。

## 修复

- 在 `SHOPRO_STATIC_URL=local` 时，将显式配置的遗留媒体源下 `/static/**` 转换为
  当前站点同源路径；
- 遗留源上的非内置上传文件使用显式配置的 `/static/goods-empty.png`，不伪造已丢失
  的业务图片；
- 遗留源和占位图进入 Mall `.env` 及 Ubuntu 26.04 H5 构建 YAML；
- API 响应拦截器递归规范化嵌套业务载荷，覆盖分类、SKU、评论、门店和头像等
  直接绑定路径；
- 审计发现的直接动态图片绑定同步补上组件级 `cdn()` 防线；
- H5 模板显式声明本地 favicon；
- 新增 `rebuild-mall` YAML 启动模式，只替换 Mall 容器。

## 验证

- URL 与嵌套 API 载荷规范化测试：7/7 通过；
- 目标代码行覆盖率：100%；
- 报告中的 13 个本地装修资源和占位图均为 `200`，且 `Content-Type` 为
  `image/png` 或 `image/jpeg`；
- Ubuntu 26.04 HBuilderX 容器构建成功；
- 新 Mall 容器已替换并通过健康检查。
- 部署入口更新为 `index-p6kTHZdU.js`，favicon 被构建为本地哈希 PNG。

Firefox 无头截图进程未正常产出截图，因此没有把截图记为通过证据。
