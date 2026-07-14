# 服务设计文档

## MITEDTSM CRM 子系统

---

## 文档信息

| 项目 | 说明 |
|------|------|
| 项目名称 | MITEDTSM（密讯ETM系统）CRM 子系统 |
| 服务架构 | 单体应用 + 模块化服务层 |
| 框架 | Spring Boot 3.5.9 |
| 参考来源 | 04-架构设计/Service_Design.md |
| 文档版本 | V2.0 |
| 创建日期 | 2026-06-25 |
| 变更说明 | V2.0 — CRM服务优先详述，HR服务后置精简（P2） |

---

## 1. 服务层架构

### 1.1 分层职责

```
┌─────────────────────────────────────────────┐
│  Controller 层 (controller/)                 │
│  职责：接收请求、参数校验、响应封装              │
├─────────────────────────────────────────────┤
│  Application Service 层 (service/)            │
│  职责：编排业务流程、事务管理、权限校验、DTO转换 │
├─────────────────────────────────────────────┤
│  Domain Service 层 (service/ 领域逻辑)        │
│  职责：核心业务逻辑、领域规则、不变性约束       │
├─────────────────────────────────────────────┤
│  Repository 层 (dal/)                         │
│  职责：数据持久化、缓存操作、外部服务调用       │
└─────────────────────────────────────────────┘
```

### 1.2 服务类命名规范

| 层级 | 命名规范 | CRM示例 |
|------|----------|---------|
| Controller | {Entity}Controller | CustomerController |
| Service | {Entity}Service / {Entity}ServiceImpl | CustomerServiceImpl |
| Mapper | {Entity}Mapper | CustomerMapper |
| Convert | {Entity}Convert | CustomerConvert |
| DO | {Entity}DO | CustomerDO |
| VO | {Entity}VO | CustomerVO |
| DTO | {Entity}DTO / {Entity}CreateDTO | CustomerCreateDTO |

---

## 2. 服务清单

### 2.1 System 模块服务（复用 — 基础支撑）

| 服务类 | 职责 |
|--------|------|
| AdminAuthService | 用户登录、认证、权限信息获取 |
| UserService | 用户 CRUD、密码管理、头像管理 |
| RoleService | 角色 CRUD、权限分配 |
| MenuService | 菜单树管理、权限标识管理 |
| DeptService | 部门树管理、组织架构 |
| DictDataService | 字典类型和数据管理 |
| TenantService | 租户管理、数据隔离 |

### 2.2 BPM 模块服务（复用 — 审批支撑）

| 服务类 | 职责 |
|--------|------|
| ProcessDefinitionService | 流程定义管理、发起流程 |
| TaskService | 任务查询、审批、驳回、转办、加签 |
| BpmCallbackService | 审批回调处理（状态同步） |

### 2.3 CRM 客户域服务（★ 新增 — P0）

| 服务类 | 职责 |
|--------|------|
| CustomerService | 客户CRUD、客户分配、客户查重 |
| ContactService | 联系人管理、首要联系人 |
| HighSeasService | 公海规则（自动掉入/领取/分配/退回） |
| CustomerAnalysisService | 客户分析（来源/行业/区域/星级/成交率/TOP10） |
| FollowRecordService | 跟进记录、客户拜访 |
| CustomerCareService | 客户关怀（生日/节假日祝福/定期回访） |
| CustomerImportExportService | 客户批量导入导出（Excel） |

### 2.4 CRM 销售域服务（★ 新增 — P0）

| 服务类 | 职责 |
|--------|------|
| LeadService | 线索CRUD、导入、清洗、转化 |
| OpportunityService | 商机CRUD、阶段推进（单向）、竞争对手 |
| QuotationService | 产品报价（产品选择/折扣/总价/报价确认） |
| OrderService | 订单CRUD、产品行管理、7种状态流转 |
| OrderNumberService | 订单编号自动生成 |
| ContractService | 合同管理（编号/签署日期/关联订单） |
| SalesFunnelService | 销售漏斗（阶段转化率/趋势分析） |
| SalesForecastService | 销售预测（基于历史数据） |

### 2.5 CRM 财务域服务（★ 新增 — P0）

| 服务类 | 职责 |
|--------|------|
| ReceivableService | 回款CRUD、回款计划、逾期检测与催款 |
| InvoiceService | 发票管理（开票/票据记录/关联订单） |
| ReimbursementService | 报销管理（CRUD + BPM审批集成） |
| RefundService | 退款管理（CRUD + BPM审批 + 金额校验） |
| ExpenseService | 费用管理（CRUD + 分类 + 关联客户/订单） |

### 2.6 CRM 工单域服务（★ 新增 — P0）

| 服务类 | 职责 |
|--------|------|
| WorkOrderService | 工单CRUD、状态机流转 |
| WorkOrderAssignService | 工单分配（自动/手动） |
| WorkOrderSLAService | SLA规则、超时检测、自动升级提醒 |
| WorkOrderStatsService | 工单统计（处理量/时效/类型/优先级） |

### 2.7 CRM 营销域服务（★ 新增 — P0）

| 服务类 | 职责 |
|--------|------|
| CampaignService | 营销活动CRUD、目标客户、效果跟踪 |
| SmsBroadcastService | 短信群发（模板管理/批量发送/审核/发送分析） |
| EmailBroadcastService | 邮件群发（模板管理/批量发送/审核/送达率分析） |
| BroadcastApprovalService | 群发审核流程（对接BPM） |
| CustomerCareRuleService | 客户关怀规则引擎（生日/节日/定期回访） |

### 2.8 CRM 办公域服务（★ 新增 — P0）

| 服务类 | 职责 |
|--------|------|
| WorkReportService | 工作报告（日报/周报/月报CRUD） |
| OaTaskService | 任务管理（CRUD + 分配/状态/优先级/进度） |
| ScheduleService | 日程管理（日/周/月视图 + Event CRUD + 提醒） |
| DocumentService | 文档管理（目录结构/CRUD/共享，对接MinIO） |
| ApprovalCenterService | 统一审批中心（聚合全部审批入口） |

### 2.9 Report 模块服务（复用 + CRM报表增强）

| 服务类 | 职责 |
|--------|------|
| CustomerReportService | 客户分析报表（来源/行业/区域/星级/成交率） |
| SalesFunnelReportService | 销售漏斗、销售预测报表 |
| SalesPerformanceReportService | 业绩排名报表（按人员/部门/时间） |
| FinanceReportService | 财务统计报表（回款/发票/报销/退款） |
| WorkOrderReportService | 工单分析报表（处理量/时效/SLA） |
| MarketingReportService | 营销效果报表（活动/群发/转化） |

---

### 2.10 HRM 模块服务（P2 — 增值功能，后置交付）

> **说明**：以下HR服务仅在CRM核心模块交付有余力时投入，整体优先级P2。

| 模块 | 服务类 | 职责 |
|------|--------|------|
| 招聘 | ResumeService, InterviewService, QuestionBankService, BlacklistService | 简历/面试/题库/黑名单 |
| 员工 | EmployeeService, MovementService | 花名册/入转调离/晋升/返聘 |
| 考勤 | AttendanceRecordService, ShiftService, LeaveService, HolidayService | 打卡/班次/假期/异常 |
| 绩效 | PerformancePlanService, PerformanceRecordService, PerformanceLevelService | 绩效等级/考核/评分 |
| 薪酬 | SalaryRuleService, SalaryRecordService, SalaryAdjustService | 核算规则/工资表/调薪 |

---

## 3. CRM服务间调用关系

```
[CustomerService] ←→ [ContactService]
[CustomerService] ←→ [HighSeasService]
[CustomerService] ←→ [FollowRecordService]

[LeadService] ──→ [CustomerService]          # 线索转化创建客户
[OpportunityService] ──→ [CustomerService]   # 商机关联客户
[OpportunityService] ──→ [LeadService]       # 线索→商机转化
[QuotationService] ──→ [ProductService]      # 报价引用产品数据
[OrderService] ──→ [CustomerService]         # 订单关联客户
[OrderService] ──→ [OpportunityService]      # 商机→订单转化
[OrderService] ──→ [ProcessDefinitionService] # 订单审批流程

[ReceivableService] ──→ [OrderService]       # 回款关联订单
[InvoiceService] ──→ [OrderService]          # 发票关联订单
[ReimbursementService] ──→ [ProcessDefinitionService] # 报销审批
[RefundService] ──→ [ReceivableService]      # 退款校验回款

[WorkOrderService] ──→ [CustomerService]     # 工单关联客户

[CampaignService] ──→ [CustomerService]      # 活动选择目标客户
[SmsBroadcastService] ──→ [SmsService]       # 短信群发对接消息中心
[EmailBroadcastService] ──→ [EmailService]   # 邮件群发对接消息中心

[ApprovalCenterService] ──→ [TaskService]    # 统一审批对接BPM
```

---

## 4. 事务管理策略

### 4.1 事务传播行为

| 场景 | 传播行为 | CRM示例 |
|------|----------|---------|
| 单一写操作 | REQUIRED（默认） | 创建客户+联系人 |
| 日志/审计 | REQUIRES_NEW | 客户操作日志独立事务 |
| 只读查询 | SUPPORTS | 客户列表查询 |
| 异步操作 | NOT_SUPPORTED | 群发短信/邮件 |

### 4.2 CRM典型事务场景

```java
@Service
public class CustomerServiceImpl {

    @Transactional(rollbackFor = Exception.class)
    public CustomerVO createCustomer(CustomerCreateDTO dto) {
        // 1. 查重校验
        customerDomainService.checkDuplicate(dto.getName(), dto.getPhone());
        
        // 2. 创建客户记录
        CustomerDO customer = customerDomainService.create(dto);
        
        // 3. 创建联系人（如果提供）
        if (dto.getContacts() != null) {
            contactService.batchCreate(customer.getId(), dto.getContacts());
        }
        
        // 4. 记录操作日志（独立事务）
        logService.recordAsync("创建客户", customer.getId());
        
        return customerConvert.toVO(customer);
    }
}
```

---

## 5. 异步处理策略（CRM场景）

| 场景 | 实现方式 | 说明 |
|------|----------|------|
| 短信/邮件群发 | @Async + 线程池 | CRM群发大批量处理 |
| 客户关怀触发 | XXL-Job 定时任务 | 生日/节假日自动发送 |
| 公海自动回收 | XXL-Job 定时任务 | 超30天无跟进自动掉入 |
| 回款逾期检测 | XXL-Job 定时任务 | 每日扫描逾期回款 |
| 操作日志记录 | @Async + 事件监听 | 异步记录不影响主流程 |
| 工单SLA超时升级 | XXL-Job 定时任务 | 超时自动升级提醒 |

---

## 6. 缓存策略（CRM场景）

### 6.1 缓存Key设计

```
crm:customer:info:{id}
crm:customer:list:{tenantId}:{page}
crm:dict:customer:status
crm:dict:customer:source
crm:dict:customer:industry
crm:opportunity:stage:{tenantId}
crm:order:status:{tenantId}
```

### 6.2 缓存更新策略

- **Cache Aside 模式**：读时加载，写时删除
- **字典数据**：定时刷新 + Redis Pub/Sub变更通知
- **客户热数据**：LRU + TTL 30分钟
- **组织架构**：变更时清除

---

## 7. 文档变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| V1.0 | 2026-06-25 | 初始版本 | 架构设计团队 |
| V2.0 | 2026-06-25 | CRM服务优先详述（6大域），HR服务后置精简（P2）；增加CRM事务/异步/缓存策略 | 架构设计团队 |
