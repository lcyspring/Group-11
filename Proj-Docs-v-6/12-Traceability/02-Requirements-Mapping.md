# 需求映射文档 - Requirements Mapping

## 1. 文档信息

| 项目 | 说明 |
|------|------|
| 项目名称 | 密讯基础平台 MIT-FMP-V1.0 |
| 文档类型 | 需求映射文档 |
| 文档版本 | V1.0 |
| 创建日期 | 2026-03-21 |
| 负责人 | 项目经理 |

> 来源: Proj-Docs/08-Traceability/Requirements_Mapping.md

---

## 2. 需求与模块映射关系

### 2.1 基础平台域（G01）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-001 | 用户登录 | 用户管理 | 登录认证 | 张伟(G01) |
| REQ-002 | Token认证 | 用户管理 | 登录认证 | 张伟(G01) |
| REQ-003 | 密码加密 | 用户管理 | 登录认证 | 张伟(G01) |
| REQ-004 | 登录锁定 | 用户管理 | 登录认证 | 张伟(G01) |
| REQ-005 | 用户列表查询 | 用户管理 | 用户管理 | 张伟(G01) |
| REQ-006 | 新增用户 | 用户管理 | 用户管理 | 张伟(G01) |
| REQ-007 | 编辑用户 | 用户管理 | 用户管理 | 张伟(G01) |
| REQ-008 | 用户状态管理 | 用户管理 | 用户管理 | 张伟(G01) |
| REQ-009 | 密码重置 | 用户管理 | 用户管理 | 张伟(G01) |
| REQ-010 | 角色CRUD | 角色管理 | 角色管理 | 张伟(G01) |
| REQ-011 | 角色权限分配 | 角色管理 | 角色管理 | 张伟(G01) |
| REQ-012 | 角色关联用户 | 角色管理 | 角色管理 | 张伟(G01) |
| REQ-013 | 菜单树管理 | 菜单管理 | 菜单管理 | 张伟(G01) |
| REQ-014 | 权限标识 | 菜单管理 | 菜单管理 | 张伟(G01) |
| REQ-015 | 部门管理 | 部门管理 | 部门管理 | 张伟(G01) |
| REQ-016 | 岗位管理 | 岗位管理 | 岗位管理 | 张伟(G01) |
| REQ-017 | 字典管理 | 字典管理 | 字典管理 | 张伟(G01) |
| REQ-018 | 字典缓存 | 字典管理 | 字典管理 | 张伟(G01) |
| REQ-019 | 租户管理 | 租户管理 | 租户管理 | 张伟(G01) |
| REQ-020 | 系统配置 | 系统配置 | 系统配置 | 张伟(G01) |
| REQ-021 | 数据权限 | 数据权限 | 数据权限 | 张伟(G01) |

### 2.2 工作流域（G02）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-022 | 流程定义 | 流程定义 | 流程定义管理 | 李娜(G02) |
| REQ-023 | 流程部署 | 流程定义 | 流程定义管理 | 李娜(G02) |
| REQ-024 | 流程实例 | 流程实例 | 流程实例管理 | 李娜(G02) |
| REQ-025 | 待办任务 | 任务管理 | 任务管理 | 李娜(G02) |
| REQ-026 | 审批通过/驳回 | 审批管理 | 审批管理 | 李娜(G02) |
| REQ-027 | 加签/减签/转办 | 审批管理 | 审批管理 | 李娜(G02) |
| REQ-028 | 批量审批 | 审批管理 | 审批管理 | 李娜(G02) |
| REQ-029 | 表单设计 | 表单设计 | 表单设计 | 李娜(G02) |
| REQ-030 | 流程监控 | 流程监控 | 流程监控 | 李娜(G02) |

### 2.3 消息中心域（G03）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-031 | 站内信 | 站内信 | 站内信 | 王强(G03) |
| REQ-032 | WebSocket推送 | 站内信 | 站内信 | 王强(G03) |
| REQ-033 | 系统公告 | 系统公告 | 系统公告 | 王强(G03) |
| REQ-034 | 短信服务 | 短信服务 | 短信服务 | 王强(G03) |
| REQ-035 | 邮件服务 | 邮件服务 | 邮件服务 | 王强(G03) |
| REQ-036 | 消息模板 | 消息模板 | 消息模板 | 王强(G03) |

### 2.4 报表域（G04）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-037 | 招聘报表 | 招聘报表 | 报表管理 | 赵敏(G04) |
| REQ-038 | 员工报表 | 员工报表 | 报表管理 | 赵敏(G04) |
| REQ-039 | 考勤报表 | 考勤报表 | 报表管理 | 赵敏(G04) |
| REQ-040 | 绩效报表 | 绩效报表 | 报表管理 | 赵敏(G04) |
| REQ-041 | 薪酬报表 | 薪酬报表 | 报表管理 | 赵敏(G04) |
| REQ-042 | 销售报表 | 销售报表 | 报表管理 | 赵敏(G04) |
| REQ-043 | 报表导出 | 报表导出 | 报表管理 | 赵敏(G04) |
| REQ-044 | 大屏设计 | 大屏设计 | 报表管理 | 赵敏(G04) |

### 2.5 招聘管理域（G05）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-045 | 简历CRUD | 简历管理 | 简历管理 | 孙磊(G05) |
| REQ-046 | 简历批量导入 | 简历管理 | 简历管理 | 孙磊(G05) |
| REQ-047 | 简历全文搜索 | 简历管理 | 简历管理 | 孙磊(G05) |
| REQ-048 | 面试安排 | 面试管理 | 面试管理 | 孙磊(G05) |
| REQ-049 | 面试评价 | 面试管理 | 面试管理 | 孙磊(G05) |
| REQ-050 | 面试题库 | 面试题库 | 面试题库 | 孙磊(G05) |
| REQ-051 | 面试黑名单 | 面试黑名单 | 黑名单管理 | 孙磊(G05) |

### 2.6 员工管理域（G06）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-052 | 花名册 | 员工花名册 | 员工管理 | 周婷(G06) |
| REQ-053 | 入职管理 | 入职管理 | 员工管理 | 周婷(G06) |
| REQ-054 | 转正管理 | 转正管理 | 员工管理 | 周婷(G06) |
| REQ-055 | 调岗管理 | 调岗管理 | 员工管理 | 周婷(G06) |
| REQ-056 | 晋升管理 | 晋升管理 | 员工管理 | 周婷(G06) |
| REQ-057 | 离职管理 | 离职管理 | 员工管理 | 周婷(G06) |
| REQ-058 | 返聘管理 | 返聘管理 | 员工管理 | 周婷(G06) |
| REQ-059 | 异动记录 | 异动记录 | 员工管理 | 周婷(G06) |

### 2.7 考勤管理域（G07）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-060 | 打卡记录 | 打卡记录 | 考勤管理 | 吴刚(G07) |
| REQ-061 | 异常处理 | 异常处理 | 考勤管理 | 吴刚(G07) |
| REQ-062 | 班次设置 | 班次设置 | 考勤管理 | 吴刚(G07) |
| REQ-063 | 假期管理 | 假期管理 | 考勤管理 | 吴刚(G07) |
| REQ-064 | 请假管理 | 请假管理 | 考勤管理 | 吴刚(G07) |
| REQ-065 | 取卡规则 | 取卡规则 | 考勤管理 | 吴刚(G07) |

### 2.8 绩效薪酬域（G08）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-066 | 绩效等级 | 绩效等级 | 绩效管理 | 郑丽(G08) |
| REQ-067 | 考核计划 | 考核计划 | 绩效管理 | 郑丽(G08) |
| REQ-068 | 考核记录 | 考核记录 | 绩效管理 | 郑丽(G08) |
| REQ-069 | 绩效工资 | 绩效工资 | 绩效管理 | 郑丽(G08) |
| REQ-070 | 薪酬规则 | 薪酬规则 | 薪酬管理 | 郑丽(G08) |
| REQ-071 | 薪酬字段 | 薪酬字段 | 薪酬管理 | 郑丽(G08) |
| REQ-072 | 工资表 | 工资表 | 薪酬管理 | 郑丽(G08) |
| REQ-073 | 调薪管理 | 调薪管理 | 薪酬管理 | 郑丽(G08) |
| REQ-074 | 薪酬计算 | 薪酬计算 | 薪酬管理 | 郑丽(G08) |

### 2.9 CRM扩展域（G09）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-075 | 公海管理 | 客户管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-076 | 客户关怀 | 客户管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-077 | 客户拜访 | 客户管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-078 | 客户分析 | 客户管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-079 | 线索管理 | 销售管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-080 | 商机管理 | 销售管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-081 | 订单管理 | 销售管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-082 | 回款管理 | 销售管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-083 | 退款管理 | 销售管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-084 | 发票管理 | 销售管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-085 | 费用报销 | 销售管理增强 | CRM扩展 | 刘洋(G09) |
| REQ-086 | 营销活动 | 营销活动 | CRM扩展 | 刘洋(G09) |
| REQ-087 | 工作报告 | 办公协作 | 办公协作 | 刘洋(G09) |
| REQ-088 | 任务管理 | 办公协作 | 办公协作 | 刘洋(G09) |
| REQ-089 | 日程管理 | 办公协作 | 办公协作 | 刘洋(G09) |
| REQ-090 | 文档管理 | 办公协作 | 办公协作 | 刘洋(G09) |

### 2.10 测试与DevOps域（G10-G11）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-091 | 测试计划 | 测试计划 | 测试管理 | 黄琳(G10) |
| REQ-092 | 测试用例 | 测试用例 | 测试管理 | 黄琳(G10) |
| REQ-093 | 接口测试 | 接口测试 | 测试管理 | 黄琳(G10) |
| REQ-094 | UI自动化 | UI自动化 | 测试管理 | 黄琳(G10) |
| REQ-095 | 性能测试 | 性能测试 | 测试管理 | 黄琳(G10) |
| REQ-096 | 安全测试 | 安全测试 | 测试管理 | 黄琳(G10) |
| REQ-097 | CI/CD | CI/CD | DevOps | 钱峰(G11) |
| REQ-098 | 容器化 | 容器化 | DevOps | 钱峰(G11) |
| REQ-099 | 监控告警 | 监控告警 | DevOps | 钱峰(G11) |
| REQ-100 | 日志收集 | 日志收集 | DevOps | 钱峰(G11) |

### 2.11 架构治理域（G12）

| 需求ID | 需求名称 | 所属模块 | 功能模块 | 负责人 |
|--------|----------|----------|----------|--------|
| REQ-101 | DDD架构 | DDD架构 | 架构治理 | 马超(G12) |
| REQ-102 | 代码规范 | 代码规范 | 架构治理 | 马超(G12) |
| REQ-103 | 代码审查 | 代码审查 | 架构治理 | 马超(G12) |
| REQ-104 | 安全审计 | 安全审计 | 架构治理 | 马超(G12) |
| REQ-105 | 质量门禁 | 质量门禁 | 架构治理 | 马超(G12) |

---

## 3. 需求与用户故事映射

| 需求ID | 用户故事 | 优先级 | 用户角色 |
|--------|----------|--------|----------|
| REQ-001 | 作为用户，我希望通过账号密码登录系统 | P0 | 所有用户 |
| REQ-002 | 作为用户，我希望登录后Token自动续期 | P0 | 所有用户 |
| REQ-005 | 作为管理员，我希望查看所有用户列表 | P0 | 系统管理员 |
| REQ-010 | 作为管理员，我希望创建和管理角色 | P0 | 系统管理员 |
| REQ-013 | 作为管理员，我希望配置系统菜单 | P0 | 系统管理员 |
| REQ-022 | 作为管理员，我希望设计审批流程模板 | P0 | 流程管理员 |
| REQ-026 | 作为审批人，我希望通过/驳回审批请求 | P0 | 审批人 |
| REQ-031 | 作为用户，我希望接收站内信通知 | P0 | 所有用户 |
| REQ-033 | 作为管理员，我希望发布系统公告 | P0 | 系统管理员 |
| REQ-037 | 作为HR，我希望查看招聘漏斗报表 | P1 | HR管理员 |
| REQ-045 | 作为HR，我希望管理候选人简历 | P0 | 招聘专员 |
| REQ-048 | 作为面试官，我希望查看面试安排 | P0 | 面试官 |
| REQ-052 | 作为HR，我希望查看员工花名册 | P0 | HR管理员 |
| REQ-053 | 作为员工，我希望完成入职流程 | P0 | 新员工 |
| REQ-060 | 作为员工，我希望进行每日打卡 | P0 | 所有员工 |
| REQ-063 | 作为员工，我希望查询假期余额 | P0 | 所有员工 |
| REQ-066 | 作为管理员，我希望配置绩效等级 | P0 | HR管理员 |
| REQ-070 | 作为HR，我希望配置薪酬核算规则 | P0 | 薪酬专员 |
| REQ-075 | 作为销售，我希望查看公海客户 | P0 | 销售人员 |
| REQ-079 | 作为销售，我希望管理销售线索 | P0 | 销售人员 |
| REQ-087 | 作为员工，我希望提交工作报告 | P1 | 所有员工 |

---

## 4. 需求与API映射

| 需求ID | API路径 | HTTP方法 | 说明 |
|--------|---------|----------|------|
| REQ-001 | /api/auth/login | POST | 用户登录 |
| REQ-001 | /api/auth/captcha | GET | 获取验证码 |
| REQ-005 | /api/system/user/list | GET | 用户列表查询 |
| REQ-006 | /api/system/user | POST | 新增用户 |
| REQ-007 | /api/system/user | PUT | 编辑用户 |
| REQ-010 | /api/system/role/list | GET | 角色列表 |
| REQ-011 | /api/system/role/permission | PUT | 角色权限分配 |
| REQ-013 | /api/system/menu/tree | GET | 菜单树 |
| REQ-015 | /api/system/dept/tree | GET | 部门树 |
| REQ-017 | /api/system/dict/data/{type} | GET | 字典数据 |
| REQ-022 | /api/bpm/definition/list | GET | 流程定义列表 |
| REQ-022 | /api/bpm/definition/deploy | POST | 部署流程 |
| REQ-024 | /api/bpm/instance/start | POST | 发起流程 |
| REQ-025 | /api/bpm/task/todo | GET | 待办任务 |
| REQ-026 | /api/bpm/task/approve | POST | 审批通过 |
| REQ-026 | /api/bpm/task/reject | POST | 审批驳回 |
| REQ-031 | /api/infra/message/send | POST | 发送站内信 |
| REQ-031 | /api/infra/message/list | GET | 消息列表 |
| REQ-033 | /api/infra/notice/list | GET | 公告列表 |
| REQ-034 | /api/infra/sms/send | POST | 发送短信 |
| REQ-035 | /api/infra/mail/send | POST | 发送邮件 |
| REQ-037 | /api/report/recruitment/{type} | GET | 招聘报表 |
| REQ-045 | /api/recruitment/resume/list | GET | 简历列表 |
| REQ-048 | /api/recruitment/interview/schedule | POST | 面试安排 |
| REQ-052 | /api/employee/list | GET | 员工列表 |
| REQ-053 | /api/employee/onboard | POST | 入职申请 |
| REQ-060 | /api/attendance/clock-in | POST | 上班打卡 |
| REQ-060 | /api/attendance/clock-out | POST | 下班打卡 |
| REQ-063 | /api/attendance/leave/balance | GET | 假期余额 |
| REQ-066 | /api/performance/grade/list | GET | 绩效等级 |
| REQ-070 | /api/salary/rule/list | GET | 薪酬规则 |
| REQ-072 | /api/salary/sheet/generate | POST | 生成工资表 |
| REQ-079 | /api/crm/lead/list | GET | 线索列表 |
| REQ-081 | /api/crm/order | POST | 创建订单 |
| REQ-087 | /api/office/report | POST | 提交工作报告 |

---

## 5. 需求与数据库表映射

| 需求ID | 数据库表 | 说明 |
|--------|----------|------|
| REQ-001~009 | sys_user | 用户表 |
| REQ-010~012 | sys_role, sys_role_menu, sys_user_role | 角色相关表 |
| REQ-013~014 | sys_menu | 菜单表 |
| REQ-015 | sys_dept | 部门表 |
| REQ-016 | sys_post | 岗位表 |
| REQ-017~018 | sys_dict_type, sys_dict_data | 字典表 |
| REQ-019 | sys_tenant | 租户表 |
| REQ-022~030 | act_ru_*, act_hi_* | Flowable流程表 |
| REQ-031~032 | infra_message | 站内信表 |
| REQ-033 | infra_notice | 公告表 |
| REQ-034 | infra_sms_log | 短信日志表 |
| REQ-035 | infra_mail_log | 邮件日志表 |
| REQ-045~047 | recruitment_resume | 简历表 |
| REQ-048~049 | recruitment_interview | 面试表 |
| REQ-050 | recruitment_question_bank, recruitment_question | 题库表 |
| REQ-051 | recruitment_blacklist | 黑名单表 |
| REQ-052~059 | employee_info, employee_change_log | 员工表、异动记录表 |
| REQ-060~061 | attendance_record | 打卡记录表 |
| REQ-062 | attendance_shift | 班次表 |
| REQ-063~064 | attendance_leave | 请假表 |
| REQ-065 | attendance_rule | 打卡规则表 |
| REQ-066~069 | performance_grade, performance_plan, performance_record | 绩效表 |
| REQ-070~074 | salary_rule, salary_field, salary_sheet, salary_adjust | 薪酬表 |
| REQ-075~078 | crm_customer, crm_customer_pool | 客户表 |
| REQ-079 | crm_lead | 线索表 |
| REQ-080 | crm_opportunity | 商机表 |
| REQ-081 | crm_order | 订单表 |
| REQ-082 | crm_payment | 回款表 |
| REQ-083~085 | crm_refund, crm_invoice, crm_expense | 退款/发票/报销表 |
| REQ-087 | office_report | 工作报告表 |
| REQ-088 | office_task | 任务表 |
| REQ-089 | office_calendar | 日程表 |
| REQ-090 | office_document | 文档表 |

---

## 6. 完整映射矩阵

| 需求ID | 模块 | 用户故事 | API | 数据库表 | 开发组 | 测试用例 |
|--------|------|----------|-----|----------|--------|----------|
| REQ-001 | 用户管理 | 用户登录 | POST /api/auth/login | sys_user | G01 | TC-LOGIN-001 |
| REQ-005 | 用户管理 | 查看用户列表 | GET /api/system/user/list | sys_user | G01 | TC-USER-001 |
| REQ-010 | 角色管理 | 管理角色 | GET /api/system/role/list | sys_role | G01 | TC-ROLE-001 |
| REQ-013 | 菜单管理 | 配置菜单 | GET /api/system/menu/tree | sys_menu | G01 | TC-MENU-001 |
| REQ-022 | 流程定义 | 设计流程 | POST /api/bpm/definition/deploy | act_re_* | G02 | TC-BPM-001 |
| REQ-026 | 审批管理 | 审批通过 | POST /api/bpm/task/approve | act_ru_task | G02 | TC-BPM-005 |
| REQ-031 | 站内信 | 接收消息 | POST /api/infra/message/send | infra_message | G03 | TC-MSG-001 |
| REQ-045 | 简历管理 | 管理简历 | GET /api/recruitment/resume/list | recruitment_resume | G05 | TC-REC-001 |
| REQ-052 | 员工花名册 | 查看花名册 | GET /api/employee/list | employee_info | G06 | TC-EMP-001 |
| REQ-060 | 打卡记录 | 上班打卡 | POST /api/attendance/clock-in | attendance_record | G07 | TC-ATT-001 |
| REQ-066 | 绩效等级 | 配置等级 | GET /api/performance/grade/list | performance_grade | G08 | TC-PERF-001 |
| REQ-070 | 薪酬规则 | 配置规则 | GET /api/salary/rule/list | salary_rule | G08 | TC-SAL-001 |
| REQ-075 | 公海管理 | 查看公海 | GET /api/crm/pool/list | crm_customer_pool | G09 | TC-CRM-001 |
| REQ-087 | 工作报告 | 提交报告 | POST /api/office/report | office_report | G09 | TC-OFFICE-001 |

---

> **文档版本**: V1.0  
> **创建日期**: 2026-03-21  
> **负责人**: 项目经理  
> **审核状态**: 待审核