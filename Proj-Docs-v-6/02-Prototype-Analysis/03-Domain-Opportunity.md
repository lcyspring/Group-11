# 商机域 (Opportunity) 分析

## 1. 子模块

### 1.1 商机管理
- 全部商机列表
- 新增商机 / 添加商机
- 商机详情 (基本信息+产品报价+跟进记录)
- 商机记录 / 商机记录报表

### 1.2 报价管理
- 产品报价管理 (产品/数量/折扣/总价)
- 确认报价
- 报价记录

### 1.3 销售分析
- 销售漏斗分析 (阶段+数量+转化率)
- 销售预测分析 / 销售预测报表
- 销售漏斗报表
- 成交商机报表 / 成交商机汇总

## 2. 核心实体

### Opportunity (商机)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| title | VARCHAR(200) | 商机标题 |
| customerId | BIGINT | 关联客户 |
| stage | VARCHAR(30) | 商机阶段 |
| source | VARCHAR(30) | 商机来源 |
| totalAmount | DECIMAL(18,2) | 报价总金额 |
| competitor | VARCHAR(100) | 竞争对手 |
| notes | TEXT | 备注 |
| ownerId | BIGINT | 负责人员 |
| lastFollowUpTime | DATETIME | 最后跟进时间 |
| daysWithoutFollowUp | INT | 未跟进天数 |
| expectedCloseDate | DATE | 预计成交日期 |
| winRate | DECIMAL(5,2) | 赢单率 |
| tenantId | BIGINT | 租户ID |

### ProductQuotation (产品报价)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| opportunityId | BIGINT | 关联商机 / 订单 |
| productId | BIGINT | 产品ID |
| productName | VARCHAR(100) | 产品名称 |
| productCode | VARCHAR(50) | 产品编号 |
| standardPrice | DECIMAL(18,2) | 标准单价 |
| actualPrice | DECIMAL(18,2) | 实际售价 |
| quantity | INT | 数量 |
| discount | DECIMAL(5,2) | 折扣(%) |
| total | DECIMAL(18,2) | 总价 |
| specifications | VARCHAR(200) | 产品规格 |
| gift | VARCHAR(100) | 礼品 |
| notes | TEXT | 备注 |

### SalesFunnel (销售漏斗阶段)

| 阶段 | 说明 |
|------|------|
| 初步接洽 | 初步接触客户 |
| 需求分析 | 挖掘需求 |
| 方案报价 | 提交方案和报价 |
| 商务谈判 | 价格/条款谈判 |
| 成交 | Won |
| 流失 | Lost |

## 3. 业务规则

1. **漏斗转化率**: 当前阶段商机数 ÷ 大于等于当前阶段商机总和（流失不计算）
2. **未跟进预警**: N天未跟进商机标红提醒
3. **赢单率**: 根据阶段和客户历史数据自动预测
4. **报价与订单关联**: 商机报价确认后可转订单

## 4. 与MITEDTSM的关系

| 能力 | 复用来源 | 复用度 |
|------|---------|--------|
| 产品数据 | unified-product 模块 | 60% (需扩展) |
| 审批流 | module-bpm (Flowable) | 80% |
| 客户关联 | 新建客户模块 | 直接依赖 |
| 统计分析 | module-report / 积木报表 | 70% |
