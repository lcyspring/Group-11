# WEB-COMP-BUG-001：快捷日期组件引用未定义状态导致页面初始化失败

## 现象

进入待办等加载共享前端资源的页面时，浏览器出现：

```text
Uncaught (in promise) ReferenceError: shortcutDays is not defined
```

Firefox 的 scroll-linked 定位提示只是性能告警，不是本次页面失败的原因。

## 根因

`ShortcutDateRangePicker` 模板和初始化逻辑均使用 `shortcutDays`，但组件从未声明该响应式状态。
初始化处理函数还被声明为异步函数，使同步异常表现为未捕获 Promise 拒绝。

## 修复

- 显式声明默认七天的 `shortcutDays` 响应式状态；
- 将纯同步的日期计算与事件发送改为同步函数；
- 增加组件源码契约测试，验证状态先于初始化存在且不再产生无意义 Promise 链。

## 分支

`develop`
