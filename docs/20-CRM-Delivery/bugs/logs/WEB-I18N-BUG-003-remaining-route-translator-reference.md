# WEB-I18N-BUG-003：静态路由残留翻译调用导致页面启动异常

- 日期：2026-07-16
- 分支：`develop`
- 级别：P0 / 回归
- 状态：已关闭

## 现象

管理端加载后浏览器报 `ReferenceError: t is not defined`，异常来自主入口脚本，页面无法稳定初始化。

## 根因

隐藏路由标题改为保存翻译键后移除了模块级翻译函数，但个人中心路由仍残留
`t('common.profile')`。原测试只禁止 `title: t('router.*')`，没有覆盖其他命名空间；Vite production build
完成模块转换但不会执行浏览器路由初始化，因此没有暴露未定义变量。

## 修复关键

- 个人中心标题改为保存 `common.profile` 翻译键；
- 门禁升级为禁止静态路由模块中的全部 `t()` 调用，不再只匹配 `router` 命名空间；
- Ubuntu 26.04 容器重新执行专项测试、ESLint 和 production build；
- 重建运行 Web 镜像并替换服务。

## 验证

- 静态路由模块残留 `t()`：0；
- 隐藏路由与审批页签契约：2/2；
- 审批前端专项自动化：6/6；
- Ubuntu 26.04 Web production build：通过。
