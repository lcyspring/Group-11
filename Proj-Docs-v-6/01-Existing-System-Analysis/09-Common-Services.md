# 通用服务与复用组件清单

## 1. 框架层通用服务

| 服务 | Starter | 说明 | CRM复用 |
|------|---------|------|---------|
| 用户认证 | mitedtsm-spring-boot-starter-security | OAuth2 Token, 登录 | 100% |
| 权限验证 | mitedtsm-spring-boot-starter-security | @PreAuthorize, @DataPermission | 100% |
| 多租户 | mitedtsm-spring-boot-starter-biz-tenant | TenantBaseDO, SQL拦截 | 100% |
| 数据权限 | mitedtsm-spring-boot-starter-biz-data-permission | 部门级数据隔离 | 90% |
| 操作日志 | BizLog SDK | @BizLog 自动记录 | 100% |
| 文件上传 | module-infra + S3 | 文件存储 (头像/附件) | 100% |
| 邮件发送 | module-infra | JavaMail | 80% |
| 短信发送 | module-system | 短信验证码/通知 | 70% |
| Excel | mitedtsm-spring-boot-starter-excel | FastExcel 导入导出 | 100% |
| 分布式锁 | Lock4j + Redisson | 防止重复提交 | 90% |
| 验证码 | AJ-Captcha | 滑块验证码 | 100% |
| 代码生成 | module-infra | 代码生成器 | 80% |
| 消息队列 | mitedtsm-spring-boot-starter-mq | RabbitMQ/RocketMQ | 80% |
| 定时任务 | mitedtsm-spring-boot-starter-job | XXL-JOB风格 | 100% |
| WebSocket | mitedtsm-spring-boot-starter-websocket | 实时推送 | 50% |
| IP解析 | mitedtsm-spring-boot-starter-biz-ip | IP2Region | 80% |
| 国际化i18n | mitedtsm-server resources/i18n/ | messages_*.properties | 100% |

## 2. module-system 核心API

| API接口 | 说明 | CRM使用场景 |
|---------|------|------------|
| AdminUserApi | 用户查询 | 负责人员/处理人选择 |
| DeptApi | 部门查询 | 客户归属部门, OA部门 |
| PostApi | 岗位查询 | 联系人职务 |
| DictDataApi | 字典数据 | 客户类型/来源/行业/状态 |
| PermissionApi | 权限查询 | 动态路由+按钮权限 |
| SmsApi | 短信发送 | 营销群发, 客户关怀 |
| NoticeApi | 通知公告 | 系统公告 |

## 3. module-bpm 工作流服务

| 能力 | 说明 | CRM使用场景 |
|------|------|------------|
| 流程定义 | BPMN 2.0 流程部署 | 审批流程定义 |
| 流程实例 | 启动/查询/审批 | 订单/回款/报销/请假审批 |
| 任务管理 | 待办/已办/转办/委派 | 审批任务 |
| 历史查询 | 已完成的流程 | 审批记录 |

## 4. 前端复用组件

| 组件 | Element Plus | 说明 |
|------|------------|------|
| Table | el-table | 数据表格 (分页/排序/选择) |
| Form | el-form | 表单 (校验/布局) |
| Dialog | el-dialog | 弹窗 |
| Tree | el-tree | 树形选择 (部门/产品分类) |
| DatePicker | el-date-picker | 日期选择 |
| Upload | el-upload | 文件上传 |
| Select | el-select | 下拉选择 (远程搜索) |
| Cascader | el-cascader | 级联选择 |
| Icon | el-icon | 图标 |
| ECharts | echarts | 图表 (漏斗/柱状/饼图/折线) |

## 5. 前端工具函数

| 工具 | 路径 | 说明 |
|------|------|------|
| request | @/utils/request | Axios 封装 (Token/加密/错误处理) |
| usePermission | @/hooks/usePermission | 权限判断 |
| useCache | @/hooks/useCache | 本地缓存 |
| dict 数据 | @/utils/dict | 字典数据加载 |
| i18n | @/locales/ | 国际化翻译 |
| download | @/utils/download | 文件下载 |
| formatTime | @/utils/formatTime | 时间格式化 |
