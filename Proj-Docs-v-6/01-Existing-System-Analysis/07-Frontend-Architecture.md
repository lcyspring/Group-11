# 前端架构分析

## 1. 前端应用总览

| 应用 | 目录 | 框架 | 端口 | 用途 |
|------|------|------|------|------|
| Admin Web | Code/Web/ | Vue 3.5 + Vite 5 | 3000 | 管理后台 |
| Admin Mobile | Code/AdminMobileFrontend/ | uni-app 3 | - | 管理端移动端 |
| Mall Frontend | Code/MallFrontend/ | uni-app | - | 客户商城 |
| Mall Android | Code/MallAndroid/ | Android | - | 商城Android端 |
| Portal Web | Code/portal-web/ | Vue 3 + Tailwind | 3001 | 公开门户 |

## 2. Admin Web 架构 (重点)

### 2.1 技术栈
```
Vue 3.5 + TypeScript + Vite 5
├── Element Plus 2 (UI组件)
├── Pinia (状态管理)
├── Vue Router 4 (路由)
├── Vue I18n 9 (国际化)
├── UnoCSS (原子化CSS)
├── Axios (HTTP)
└── ECharts 5 (图表)
```

### 2.2 目录结构
```
src/
├── api/             # API 接口定义 (按模块)
├── assets/          # 静态资源
├── components/      # 公共组件
├── hooks/           # 组合式函数
├── locales/         # 国际化文件 (zh-CN/en-US/ar-SA/...)
├── layout/          # 布局组件
├── plugins/         # 插件配置
├── router/          # 路由配置 (动态路由)
├── store/           # Pinia Store
├── styles/          # 全局样式
├── utils/           # 工具函数
└── views/           # 页面视图 (按模块)
```

### 2.3 动态路由机制
1. 登录后调用 `/admin-api/system/auth/get-permission-info`
2. 返回菜单树 + 权限列表
3. 前端根据菜单树动态生成路由
4. 按钮权限通过 `v-permission` 指令控制

### 2.4 请求封装 (Axios)
- 请求拦截: 添加 Token、加密请求体
- 响应拦截: 解密响应体、统一错误处理、Token 过期刷新
- 加密: AES (可配置开关)

### 2.5 代理配置 (开发环境)
```typescript
// vite.config.ts
proxy: {
  '/admin-api': 'http://localhost:8080',
  '/app-api': 'http://localhost:8080',
}
```

## 3. Admin Mobile 架构

```
uni-app 3 + Vue 3 + Vite
├── wot-design-uni (UI组件)
├── z-paging (分页)
├── Pinia (状态管理)
├── Vue I18n (国际化)
└── UniBest (模板框架)
```

## 4. Portal Web 架构

```
Vue 3 + Tailwind CSS + TypeScript
├── Vue Router (路由)
├── Vue I18n 9 (国际化, 含RTL支持)
└── Axios (HTTP, 代理到 localhost:8080)
```

## 5. 对CRM系统的前端建议

1. **复用 Admin Web 框架**: 新 CRM 模块作为 Web 的 `views/crm/` 目录
2. **复用动态路由**: 新增 CRM 菜单在 system_menu 表注册
3. **复用请求封装**: 使用现有 Axios 实例和加密方案
4. **复用国际化**: 在 `locales/` 下新增 CRM 相关翻译
5. **组件复用**: Element Plus 表格/表单/弹窗组件直接使用
6. **新前端应用**: 如需独立的 CRM 前端，可参考 portal-web 搭建
