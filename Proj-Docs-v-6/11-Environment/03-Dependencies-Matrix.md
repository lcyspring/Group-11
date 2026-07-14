# 核心依赖矩阵

## 1. 后端依赖 (Maven pom.xml)

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.9 | 核心框架 |
| MyBatis Plus | 3.5.15 | ORM |
| MyBatis Plus Join | 1.5.5 | 联表查询 |
| Druid | 1.2.27 | 连接池 |
| Dynamic Datasource | 4.5.0 | 多数据源 |
| Redisson | 3.52.0 | Redis 客户端 |
| Flowable | 7.2.0 | 工作流引擎 |
| MapStruct | 1.6.3 | 对象转换 |
| Lombok | 1.18.42 | 代码简化 |
| Hutool | 5.8.42 + 6.0.0-M22 | 工具集 |
| FastExcel | 1.3.0 | Excel |
| Knife4j | 4.5.0 | API 文档 |
| AJ-Captcha | 1.4.0 | 验证码 |
| Lock4j | 2.2.7 | 分布式锁 |
| BizLog | 3.0.6 | 操作日志 |
| SkyWalking | 9.5.0 | APM |
| JimuReport | 2.1.3 | 报表 |
| AWS SDK S3 | 2.40.15 | 对象存储 |
| RocketMQ Spring | 2.3.5 | 消息队列 |

## 2. Web 前端依赖 (package.json)

| 依赖 | 版本 | 用途 |
|------|------|------|
| vue | 3.5.12 | 核心框架 |
| vue-router | 4.4.5 | 路由 |
| pinia | ^2.1.7 | 状态管理 |
| vue-i18n | 9.10.2 | 国际化 |
| element-plus | 2.11.1 | UI 组件库 |
| echarts | ^5.5.0 | 图表 |
| axios | 1.9.0 | HTTP 客户端 |
| crypto-js | ^4.2.0 | 加密 |
| @wangeditor-next/editor | ^5.6.46 | 富文本 |
| vite | 5.1.4 | 构建工具 |
| unocss | ^0.58.5 | 原子化 CSS |
| typescript | 5.3.3 | 类型系统 |

## 3. AdminMobile 前端依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| uni-app | 3.0.0-4070620250821001 | 跨平台框架 |
| vue | ^3.4.21 | 核心框架 |
| pinia | 2.0.36 | 状态管理 |
| wot-design-uni | ^1.13.0 | UI 组件库 |
| z-paging | 2.8.7 | 分页组件 |
| vite | 5.2.8 | 构建工具 |
| pnpm | >=9 (packageManager: 10.10.0) | 包管理 |

## 4. Portal Web 依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| vue | ^3.4.27 | 核心框架 |
| vite | ^5.2.11 | 构建工具 |
| tailwindcss | ^3.4.4 | CSS 框架 |
| vue-i18n | ^9.13.1 | 国际化 |
| lucide-vue-next | ^0.395.0 | 图标 |

## 5. Maven 仓库镜像

优先级: 华为云 > 阿里云 > Spring Milestones > Spring Snapshots

## 6. NPM 镜像

```
registry = https://registry.npmmirror.com
```
