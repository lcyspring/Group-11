# MALL-H5-BUG-004：快捷菜单客服图标返回 404

## 现象

商城 H5 打开快捷菜单时请求 `/static/img/shop/tools/service.png`，运行服务返回 HTTP 404，
客服入口显示为缺失图片。

## 根因

快捷菜单引用了从未存在于 `MallFrontend/static` 的文件名；旧构建流程只检查产物目录非空，
没有核对源码中的静态路径是否真实进入构建产物。

## 修复

- 客服入口复用项目已有的 `/static/img/diy/chat.png` 受管图标；
- Mall H5 构建测试增加快捷菜单静态字面路径扫描，同时校验源码文件和最终 H5 产物；
- 任一快捷入口图标缺失时构建验收直接失败并列出路径。

## 验证

Ubuntu 26.04 HBuilderX 无 GUI 构建通过，产出 167 个资源；七个快捷入口静态路径门禁通过。
最终 8082 部署产物不再包含旧 `service.png` 引用，新客服图标返回 HTTP 200、`image/png`。

## 分支

`develop`
