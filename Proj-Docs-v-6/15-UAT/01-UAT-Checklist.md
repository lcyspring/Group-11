# UAT 验收测试 — 详细测试计划、用例、数据与配置

## 1. UAT 测试概述

### 1.1 测试范围

| 域 | 测试模块 | US覆盖 | 测试用例数 | 优先级 |
|----|---------|:------:|:--------:|:------:|
| 客户域 | 客户/联系人/公海/分析 | CUS-001~020 | 38 | P0 |
| 商机域 | 商机/报价/漏斗/跟进 | OPP-001~011 | 24 | P0 |
| 订单域 | 订单/审批/合同 | ORD-001~008 | 20 | P0 |
| 财务域 | 回款/发票/报销/退款 | FIN-001~010 | 26 | P0 |
| 工单域 | 工单/SLA/知识库 | WO-001~006 | 16 | P1 |
| 营销域 | 活动/短信/邮件/关怀 | MKT-001~010 | 22 | P1 |
| OA域 | 请假/出差/借款/拜访/报告/任务/日程 | OA-001~015 | 34 | P1 |
| 通用 | 权限/多租户/i18n/性能 | - | 12 | P0 |
| **合计** | | **80 US** | **192** | |

### 1.2 UAT 阶段划分

```
Phase 1 (Sprint 4): 工单域 + 客户域基础 UAT     ← 最早交付
Phase 2 (Sprint 6): 客户域全量 + 商机域 UAT
Phase 3 (Sprint 8): 订单域 + 商机域全量 UAT
Phase 4 (Sprint 9): 财务域 + 营销域 UAT
Phase 5 (Sprint 10): OA域 + 审批中心 + 全量回归 UAT
```

### 1.3 UAT 参与角色

| 角色 | 人数 | 职责 |
|------|:----:|------|
| UAT 测试经理 | 1 (T10) | 测试计划、进度跟踪、缺陷管理 |
| 业务验收人员 | 3 | 按原型核对业务功能、体验验收 |
| QA 测试工程师 | 4 (各域QA) | 用例执行、缺陷记录 |
| 开发支持 | 2 (对应域TL) | 缺陷修复协调、环境问题排查 |
| DevOps | 1 (T1) | UAT 环境维护、部署 |

---

## 2. UAT 环境配置

### 2.1 环境架构

```
┌─────────────────────────────────────────────────────┐
│                   UAT 环境 (独立部署)                  │
├────────────┬────────────┬────────────┬────────────────┤
│ Nginx:80   │ Backend:48080 │ MySQL:3306 │ Redis:6379   │
│ (Web前端)   │ (Spring Boot) │ (uat_db)   │ (db=1)       │
├────────────┼────────────┼────────────┼────────────────┤
│ RabbitMQ   │ MinIO:9000 │ Flowable   │ Mock SMS/Email │
│ (uat_vhost) │ (文件存储)   │ (BPM引擎)   │ (WireMock)    │
└────────────┴────────────┴────────────┴────────────────┘
```

### 2.2 Docker Compose 部署配置

```yaml
# InstallPackage/uat/docker-compose.yaml
version: '3.8'

services:
  mysql-uat:
    image: mysql:8.0
    container_name: mitedtsm-mysql-uat
    environment:
      MYSQL_ROOT_PASSWORD: uat_root_2026
      MYSQL_DATABASE: mitedtsm_uat
      MYSQL_CHARSET: utf8mb4
      MYSQL_COLLATION: utf8mb4_unicode_ci
    ports:
      - "3307:3306"
    volumes:
      - ./mysql-uat-data:/var/lib/mysql
      - ../../InstallPackage/database/base:/docker-entrypoint-initdb.d
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=+08:00

  redis-uat:
    image: redis:6-alpine
    container_name: mitedtsm-redis-uat
    ports:
      - "6380:6379"
    command: redis-server --requirepass uat_redis_2026 --db 1

  rabbitmq-uat:
    image: rabbitmq:3-management-alpine
    container_name: mitedtsm-rabbitmq-uat
    environment:
      RABBITMQ_DEFAULT_USER: uat_user
      RABBITMQ_DEFAULT_PASS: uat_pass_2026
      RABBITMQ_DEFAULT_VHOST: uat_vhost
    ports:
      - "5673:5672"
      - "15673:15672"

  backend-uat:
    image: mitedtsm-server:uat
    container_name: mitedtsm-backend-uat
    environment:
      SPRING_PROFILES_ACTIVE: uat
      DB_HOST: mysql-uat
      DB_PORT: 3306
      DB_NAME: mitedtsm_uat
      DB_USER: root
      DB_PASS: uat_root_2026
      REDIS_HOST: redis-uat
      REDIS_PORT: 6379
      REDIS_PASS: uat_redis_2026
      RABBITMQ_HOST: rabbitmq-uat
      RABBITMQ_PORT: 5672
      RABBITMQ_USER: uat_user
      RABBITMQ_PASS: uat_pass_2026
      RABBITMQ_VHOST: uat_vhost
      LOG_LEVEL: DEBUG
    ports:
      - "48080:48080"
    depends_on:
      - mysql-uat
      - redis-uat
      - rabbitmq-uat

  frontend-uat:
    image: mitedtsm-web:uat
    container_name: mitedtsm-web-uat
    ports:
      - "8080:80"
    environment:
      VITE_API_BASE_URL: http://backend-uat:48080
    depends_on:
      - backend-uat

  wiremock-uat:
    image: wiremock/wiremock:3-alpine
    container_name: wiremock-uat
    ports:
      - "9999:8080"
    volumes:
      - ./wiremock/stubs:/home/wiremock/mappings
```

### 2.3 Backend UAT 配置

```yaml
# application-uat.yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: ${DB_USER}
    password: ${DB_PASS}
    hikari:
      max-pool-size: 10
      min-idle: 5

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASS}
      database: 1

  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT}
    username: ${RABBITMQ_USER}
    password: ${RABBITMQ_PASS}
    virtual-host: ${RABBITMQ_VHOST}

  # UAT 环境关闭API加密方便调试
  api:
    encrypt:
      enabled: false

# 日志: UAT环境开启DEBUG
logging:
  level:
    com.meession.etm.module.crm: DEBUG
    com.meession.etm.module.bpm: DEBUG

# Flowable BPM: UAT环境使用独立数据库schema
flowable:
  database-schema-update: true
  async-executor-activate: true

# Mock服务: SMS/邮件在UAT环境使用WireMock
crm:
  sms:
    provider: mock
    mock-url: http://wiremock-uat:8080
  email:
    provider: mock
    mock-url: http://wiremock-uat:8080

# Knife4j: UAT环境也开启API文档方便测试
knife4j:
  enable: true
```

### 2.4 WireMock SMS/Email Mock 配置

```json
// wiremock/stubs/sms-send-success.json
{
  "request": {
    "method": "POST",
    "urlPath": "/sms/send"
  },
  "response": {
    "status": 200,
    "jsonBody": {
      "code": 0,
      "msg": "success",
      "data": { "sendId": "mock-{{randomValue length=32 type='ALPHANUMERIC'}}", "status": "SENT" }
    },
    "headers": { "Content-Type": "application/json" }
  }
}
```

```json
// wiremock/stubs/sms-send-fail.json (模拟发送失败 - 用于异常场景测试)
{
  "request": {
    "method": "POST",
    "urlPath": "/sms/send",
    "bodyPatterns": [{ "contains": "FAIL_TRIGGER" }]
  },
  "response": {
    "status": 200,
    "jsonBody": {
      "code": 500,
      "msg": "短信发送失败: 余额不足",
      "data": null
    }
  }
}
```

---

## 3. 测试数据准备

### 3.1 基础数据准备 SQL

```sql
-- ==========================================
-- UAT 基础测试数据
-- 执行顺序: 租户 → 用户 → 部门 → 字典 → 测试业务数据
-- ==========================================

-- 1. 创建 UAT 测试租户
INSERT INTO `system_tenant` (`id`, `name`, `code`, `status`, `package_id`, `expire_time`)
VALUES (1001, 'UAT测试企业', 'uat-test-corp', 1, 1, '2027-12-31 23:59:59');

-- 2. 创建测试用户 (密码: uat123456, BCrypt加密)
-- admin / sales_manager / sales1 / sales2 / finance1 / support1 / marketing1 / employee1 / approver1
INSERT INTO `system_users` (`username`, `password`, `nickname`, `status`, `tenant_id`)
VALUES
('uat_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'UAT系统管理员', 1, 1001),
('uat_sales_mgr', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'UAT销售经理', 1, 1001),
('uat_sales1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'UAT销售员张三', 1, 1001),
('uat_sales2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'UAT销售员李四', 1, 1001),
('uat_finance1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'UAT财务赵六', 1, 1001),
('uat_support1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'UAT客服孙七', 1, 1001),
('uat_marketing1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'UAT市场周八', 1, 1001),
('uat_employee1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'UAT员工吴九', 1, 1001),
('uat_approver1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'UAT审批人郑十', 1, 1001);

-- 3. 创建测试部门
INSERT INTO `system_dept` (`name`, `parent_id`, `sort`, `tenant_id`)
VALUES
('销售部', 0, 1, 1001),
('财务部', 0, 2, 1001),
('客服部', 0, 3, 1001),
('市场部', 0, 4, 1001);

-- 4. 初始化客户域字典数据
INSERT INTO `system_dict_type` (`name`, `code`, `status`, `tenant_id`)
VALUES
('客户状态', 'crm_customer_status', 1, 1001),
('客户来源', 'crm_customer_source', 1, 1001),
('所属行业', 'crm_industry', 1, 1001);

INSERT INTO `system_dict_data` (`dict_type_id`, `label`, `value`, `sort`, `tenant_id`)
VALUES
((SELECT id FROM system_dict_type WHERE code='crm_customer_status'), '正常', '1', 1, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_customer_status'), '储备', '2', 2, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_customer_status'), '淘汰', '3', 3, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_customer_source'), '线上推广', '1', 1, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_customer_source'), '线下活动', '2', 2, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_customer_source'), '电话营销', '3', 3, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_customer_source'), '客户推荐', '4', 4, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_industry'), '信息技术', '1', 1, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_industry'), '金融', '2', 2, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_industry'), '制造业', '3', 3, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_industry'), '教育', '4', 4, 1001),
((SELECT id FROM system_dict_type WHERE code='crm_industry'), '医疗', '5', 5, 1001);

-- 5. 初始化BPM审批流定义 (通过Flowable API部署, 此处仅记录)
-- 审批流: 订单审批 / 回款审批 / 报销审批 / 退款审批 / 群发审核 / 请假审批 /
--         出差审批 / 借款审批 / 客户拜访审批 / 请示审批

-- 6. 初始化CRM菜单 (由InitService或Sprint启动脚本执行)
```

### 3.2 测试业务数据准备 (Python 脚本)

```python
#!/usr/bin/env python3
"""
UAT 测试数据生成器
用途: 批量生成CRM各域测试数据
用法: python3 uat_data_generator.py --domain customer --count 50
"""
import requests
import random
import json
import argparse
from datetime import datetime, timedelta

BASE_URL = "http://localhost:48080"
TOKEN = None  # 登录后填充

COMPANY_PREFIXES = ["XX", "YY", "ZZ", "AA", "BB", "CC", "DD", "EE"]
COMPANY_SUFFIXES = ["科技", "金融", "制造", "教育", "医疗", "贸易", "咨询", "物流"]
CONTACT_SURNAMES = ["张", "李", "王", "赵", "孙", "周", "吴", "郑", "冯", "陈"]
CONTACT_GIVEN_NAMES = ["伟", "芳", "娜", "秀英", "敏", "静", "丽", "强", "磊", "洋"]
PROVINCES = ["广东省", "浙江省", "江苏省", "北京市", "上海市", "四川省", "湖北省"]


def login(username="uat_admin", password="uat123456"):
    """登录获取Token"""
    resp = requests.post(f"{BASE_URL}/admin-api/system/auth/login", json={
        "username": username, "password": password
    })
    global TOKEN
    TOKEN = resp.json()["data"]["accessToken"]
    return TOKEN


def headers():
    return {"Authorization": f"Bearer {TOKEN}", "Content-Type": "application/json"}


def generate_customer(index):
    """生成单个客户数据"""
    return {
        "name": f"{random.choice(COMPANY_PREFIXES)}{random.choice(COMPANY_SUFFIXES)}有限公司-{index:04d}",
        "status": random.choice([1, 1, 1, 2, 3]),  # 大部分正常
        "source": random.choice([1, 2, 3, 4, 5]),
        "industry": random.choice([1, 2, 3, 4, 5]),
        "starRating": random.choice([1, 2, 3, 3, 4, 4, 5]),
        "country": "中国",
        "province": random.choice(PROVINCES),
        "city": "测试市",
        "ownerId": random.choice([10002, 10003, 10004]),
        "notes": f"UAT自动生成测试数据-{datetime.now().strftime('%Y%m%d')}"
    }


def batch_create_customers(count=50):
    """批量创建客户"""
    created = []
    for i in range(1, count + 1):
        data = generate_customer(i)
        resp = requests.post(f"{BASE_URL}/admin-api/crm/customer/create",
                             headers=headers(), json=data)
        if resp.json()["code"] == 0:
            created.append(resp.json()["data"])
            print(f"[{i}/{count}] 创建客户: {data['name']} → ID={resp.json()['data']}")
        else:
            print(f"[{i}/{count}] 创建失败: {resp.json()['msg']}")
    return created


def batch_create_opportunities(customer_ids, count=30):
    """批量创建商机"""
    stages = ["LEAD", "NEEDS", "PROPOSAL", "NEGOTIATION", "CLOSED_WON"]
    created = []
    for i in range(1, count + 1):
        data = {
            "title": f"UAT商机-{datetime.now().strftime('%Y%m')}-{i:03d}",
            "customerId": random.choice(customer_ids),
            "stage": random.choice(stages),
            "source": random.choice([1, 2, 3]),
            "totalAmount": random.randint(10000, 500000),
            "expectedCloseDate": (datetime.now() + timedelta(days=random.randint(10, 90))).strftime("%Y-%m-%d"),
            "ownerId": random.choice([10002, 10003, 10004])
        }
        resp = requests.post(f"{BASE_URL}/admin-api/crm/opportunity/create",
                             headers=headers(), json=data)
        if resp.json()["code"] == 0:
            created.append(resp.json()["data"])
        else:
            print(f"创建商机失败: {resp.json()['msg']}")
    print(f"批量创建商机: {len(created)}/{count} 成功")
    return created


def prepare_public_sea_data(customer_ids, days_without_followup=30):
    """
    准备公海测试数据: 直接将部分客户的最后跟进时间设为N天前
    (在实际UAT中通过SQL直接修改更方便)
    """
    sql = f"""
    UPDATE crm_customer
    SET last_follow_up_time = DATE_SUB(NOW(), INTERVAL {days_without_followup} DAY)
    WHERE id IN ({','.join(map(str, customer_ids[:10]))})
      AND tenant_id = 1001;
    """
    print("公海测试数据SQL:")
    print(sql)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--domain", default="customer", choices=["customer", "opportunity", "all"])
    parser.add_argument("--count", type=int, default=50)
    args = parser.parse_args()

    login()
    print(f"Token: {TOKEN[:20]}...")

    if args.domain in ("customer", "all"):
        customers = batch_create_customers(args.count)
        print(f"\n客户创建完成: {len(customers)} 条")

    if args.domain in ("opportunity", "all"):
        # 先获取已有客户ID列表
        resp = requests.post(f"{BASE_URL}/admin-api/crm/customer/page",
                             headers=headers(), json={"pageNo": 1, "pageSize": 100})
        customer_ids = [c["id"] for c in resp.json()["data"]["list"]]
        if customer_ids:
            opportunities = batch_create_opportunities(customer_ids, args.count)
```

### 3.3 测试数据准备清单

| 数据类型 | 数量 | 用途 | 准备方式 | 负责人 |
|---------|:----:|------|---------|:------:|
| 租户 | 3 | 多租户隔离测试 | SQL初始化 | DevOps |
| 测试用户 (各角色) | 9 | 权限测试 | SQL初始化 | DevOps |
| 部门 | 4 | 数据权限测试 | SQL初始化 | DevOps |
| 字典数据 | 30+ | 下拉选项验证 | SQL初始化 | DevOps |
| 客户 | 50 | 客户域UAT | Python脚本批量 | QA |
| 客户(公海) | 10 | 公海机制测试 | SQL修改 follow_up 时间 | QA |
| 联系人 | 30+ | 联系人管理测试 | 随客户创建 | QA |
| 商机 | 30 | 商机域UAT | Python脚本批量 | QA |
| 报价行 | 20+ | 报价管理测试 | 随商机创建 | QA |
| 订单 | 20 | 订单域UAT | 手动创建(含审批流测试) | QA |
| BPM流程定义 | 10 | 审批流测试 | Flowable API部署 | T9 BPM |
| 回款 | 15 | 财务域UAT | 手动创建 | QA |
| 工单 | 20 | 工单域UAT | Python脚本批量 | QA |
| 营销活动 | 5 | 营销域UAT | 手动创建 | QA |
| OA申请(各类型) | 每种5 | OA域UAT | 手动创建+审批 | QA |

---

## 4. 详细测试用例

### 4.1 客户域测试用例 (38条)

#### TC-CUS-001: 客户列表 - 默认分页加载

| 项目 | 内容 |
|------|------|
| **US** | CUS-001 |
| **优先级** | P0 |
| **前置条件** | 已登录为 uat_sales1, 数据库中有50+条客户数据 |
| **测试数据** | 50条预置客户(通过Python脚本生成) |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 登录后点击菜单 "CRM → 客户管理" | 页面加载, 显示客户列表 |
| 2 | 等待列表加载完成 | 默认第1页, 每页20条, 按创建时间倒序 |
| 3 | 检查表格列 | 显示: 客户名称, 状态, 首联系人, 手机, 行业, 来源, 创建时间, 归属 |
| 4 | 点击底部分页器 | 可切换页码, 可切换每页条数(10/20/50/100) |
| 5 | 切换到第3页 | 显示第3页数据, URL参数同步变化 |

**边界条件**:
| 场景 | 操作 | 预期结果 |
|------|------|---------|
| 空数据 | 使用新租户登录(无客户数据) | 显示空状态插画 + "暂无数据" |
| 超大数据量 | 创建200条客户, 切换最后一页 | 分页正确, 加载时间<1s |
| 网络异常 | 断网后进入页面 | 显示网络错误提示+重试按钮 |

---

#### TC-CUS-002: 客户列表 - 多条件组合搜索

| 项目 | 内容 |
|------|------|
| **US** | CUS-001 |
| **优先级** | P0 |
| **前置条件** | 已登录, 50+条客户数据 |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 在搜索栏输入客户名称关键词 "XX科技" | - |
| 2 | 下拉选择客户状态 "正常" | - |
| 3 | 下拉选择客户来源 "线上推广" | - |
| 4 | 点击 [查询] | 列表刷新, 显示同时满足名称+状态+来源的客户 |
| 5 | 点击 [重置] | 所有筛选条件清空, 列表恢复默认(第1页) |
| 6 | 选择创建时间范围 "2026-03-01 ~ 2026-03-30" | 只显示该时间段创建的客户 |

**边界条件**:
| 场景 | 操作 | 预期结果 |
|------|------|---------|
| 无匹配结果 | 搜索"不存在的客户名称XYZ123" | 列表为空, 显示"暂无数据" |
| 仅选择状态不输入名称 | 选择状态=淘汰, 点击查询 | 显示所有淘汰状态客户 |
| 时间范围倒置 | 开始时间 > 结束时间 | 前端校验提示"开始时间不得晚于结束时间" |

---

#### TC-CUS-003: 创建客户 - 全部字段填写

| 项目 | 内容 |
|------|------|
| **US** | CUS-002 |
| **优先级** | P0 |
| **前置条件** | 已登录为 uat_sales1 |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 点击 [创建客户] 按钮 | 弹出创建表单对话框 |
| 2 | 填写客户名称 "UAT测试科技公司" | - |
| 3 | 选择客户状态 "正常" | - |
| 4 | 选择客户来源 "线上推广" | - |
| 5 | 选择所属行业 "信息技术" | - |
| 6 | 点击星级 ★★★ | 星级显示为3星 |
| 7 | 选择国家"中国" → 省份"广东省" → 城市"深圳市" | 级联选择正常工作 |
| 8 | 填写详细地址 "南山区科技园100号" | - |
| 9 | 填写备注 "UAT验收测试创建的客户" | - |
| 10 | 点击 [确定] | 提示"创建成功", 列表刷新显示新客户 |

**必填校验**:
| 场景 | 操作 | 预期结果 |
|------|------|---------|
| 空名称 | 不填客户名称直接提交 | 红色提示"客户名称不能为空" |
| 超长名称 | 输入101个字符的名称 | 提示"客户名称最长100个字符" |
| 特殊字符 | 名称输入 `<script>alert(1)</script>` | XSS过滤, 正常保存或提示非法字符 |

---

#### TC-CUS-004: 客户查重 - 同名拦截

| 项目 | 内容 |
|------|------|
| **US** | CUS-003 |
| **优先级** | P0 |
| **前置条件** | 已有客户 "XX科技有限公司" |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 新建客户, 名称填入 "XX科技有限公司" | - |
| 2 | 点击确定提交 | 提示"客户名称已存在, 是否继续创建?" |
| 3 | 点击"继续创建" | 客户创建成功(允许同名但提醒) |
| 4 | 再次创建 "XX科技有限公司" + 相同手机号 | 提示"检测到疑似重复客户: 名称和手机号均匹配" |

---

#### TC-CUS-005: 公海 - 客户自动掉入

| 项目 | 内容 |
|------|------|
| **US** | CUS-010 |
| **优先级** | P0 |
| **前置条件** | 公海规则配置: 未跟进30天自动掉入; 测试客户最后跟进时间为31天前 |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 确认测试客户CUS-SEA-01的 last_follow_up_time = 31天前 | SQL查询确认 |
| 2 | 手动触发公海定时任务 (或等待凌晨2:00) | `curl -X POST /admin-api/crm/sea/trigger-job` |
| 3 | 查询测试客户的 in_sea 字段 | `in_sea = 1` |
| 4 | 该客户原负责人查看客户列表 | 该客户不再显示在个人列表中 |
| 5 | 进入公海页面 | 该客户出现在公海池中 |

---

#### TC-CUS-006: 公海 - 领取客户

| 项目 | 内容 |
|------|------|
| **US** | CUS-011 |
| **优先级** | P0 |
| **前置条件** | 公海中有可领取客户; 当前用户为 uat_sales2 |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 进入公海页面, 找到客户 "XX科技" | 公海列表显示该客户 |
| 2 | 点击 [领取] 按钮 | 弹出确认提示"确认领取该客户?" |
| 3 | 点击确认 | 提示"领取成功", 该客户从公海消失 |
| 4 | 查看个人客户列表 | 该客户出现在列表中, owner=当前用户, in_sea=0 |

**边界条件**:
| 场景 | 操作 | 预期结果 |
|------|------|---------|
| 每日上限 | 配置每人每天最多领5个; 领取第6个时 | 提示"今日领取已达上限(5个)" |
| 并发领取 | 两人同时领取同一客户 | 后提交者提示"该客户已被领取" |

---

#### TC-CUS-007: 客户导入 - Excel 批量导入

| 项目 | 内容 |
|------|------|
| **US** | CUS-016 |
| **优先级** | P1 |
| **前置条件** | 准备符合模板格式的Excel文件(30条客户数据) |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 点击 [导入] 按钮 | 弹出导入对话框, 可下载模板 |
| 2 | 点击 [下载模板] | 下载Excel模板(含表头: 客户名称/状态/来源/行业/省/市/地址/备注) |
| 3 | 选择准备好的30条客户数据Excel | 文件名校验通过(.xlsx) |
| 4 | 点击 [上传并导入] | 显示导入进度, 完成后提示"成功导入30条, 失败0条" |
| 5 | 返回列表刷新 | 新导入的30条客户出现在列表中 |

**边界条件**:
| 场景 | 操作 | 预期结果 |
|------|------|---------|
| 空文件 | 上传空Excel | 提示"文件中无数据" |
| 格式错误 | 上传.txt文件 | 提示"仅支持.xlsx格式" |
| 部分失败 | Excel中有2条名称重复 | "成功导入28条, 失败2条", 可下载失败明细 |
| 超大数据 | 上传5000条客户 | 异步处理, 显示"导入中, 请稍后查看结果" |

---

#### TC-CUS-008: 客户分析 - 区域分布

| 项目 | 内容 |
|------|------|
| **US** | CUS-019 |
| **优先级** | P1 |
| **前置条件** | 客户数据覆盖广东省(20)、浙江省(10)、江苏省(5)、北京市(15) |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 点击 "客户分析 → 区域分布" | 页面加载, 显示中国地图 |
| 2 | 查看地图热力 | 广东省颜色最深(20个客户), 江苏省最浅(5个) |
| 3 | 鼠标悬停广东省 | Tooltip显示: "广东省: 20个客户(40%)" |
| 4 | 点击地图下方 "切换为表格视图" | 显示省份排序表格(按客户数降序) |

---

### 4.2 商机域测试用例 (24条, 关键用例摘录)

#### TC-OPP-001: 商机阶段流转

| 项目 | 内容 |
|------|------|
| **US** | OPP-003 |
| **优先级** | P0 |
| **前置条件** | 商机处于"初步接触"阶段 |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 查看商机详情, 当前阶段显示"初步接触" | 阶段标签蓝色背景 |
| 2 | 点击 [推进阶段] → 选择 "需求分析" | 弹出输入框要求填写推进说明 |
| 3 | 填写 "已与客户CTO完成产品介绍, 进入需求梳理阶段" | - |
| 4 | 点击确认 | 提示"阶段推进成功", 阶段标签更新为"需求分析" |
| 5 | 查看阶段变更记录 | 显示一条变更记录: 初步接触→需求分析, 含时间+操作人+说明 |

**非法流转校验**:
| 场景 | 操作 | 预期结果 |
|------|------|---------|
| 跳过阶段 | 从"初步接触"直接跳到"谈判" | 提示"阶段流转不合法, 必须按顺序推进" |
| 从终态推进 | 商机已成交, 再次推进 | 提示"已成交商机不可推进" |
| 从终态回退 | 商机已输单, 尝试回退 | 默认不允许回退(除非管理员操作) |

---

#### TC-OPP-002: 销售漏斗数据准确性

| 项目 | 内容 |
|------|------|
| **US** | OPP-008 |
| **优先级** | P0 |
| **前置条件** | 准备已知数量的商机: LEAD=20, NEEDS=12, PROPOSAL=8, NEGOTIATION=4, CLOSED_WON=3 |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 点击 "销售漏斗" | 显示漏斗图(ECharts) |
| 2 | 查看各阶段数量 | LEAD=20, NEEDS=12, PROPOSAL=8, NEGOTIATION=4, CLOSED_WON=3 |
| 3 | 查看转化率 | NEEDS转化率=60%(12/20), PROPOSAL=66.7%(8/12), NEGOTIATION=50%(4/8), 成交=75%(3/4) |
| 4 | 查看各阶段金额 | 金额与商机总金额一致 |
| 5 | 切换时间范围"本月" | 只统计本月创建的商机漏斗 |

---

#### TC-OPP-003: 商机成交→自动创建订单

| 项目 | 内容 |
|------|------|
| **US** | OPP-010 |
| **优先级** | P0 |
| **前置条件** | 商机处于"谈判"阶段, 有报价行(2个产品), 总金额100000 |

**测试步骤**:
| 步骤 | 操作 | 预期结果 |
|:----:|------|---------|
| 1 | 点击 [成交] | 弹出确认框, 需选择成交金额和预计签约日期 |
| 2 | 填写成交金额 95000 (打了95折) | - |
| 3 | 点击确认成交 | 商机状态变为"已成交"(CLOSED_WON) |
| 4 | 自动跳转到订单模块 | 生成了新订单, 状态="草稿", 客户+产品行已自动填充 |
| 5 | 查看订单产品行 | 2个产品, 总金额95000, 与成交时一致 |
| 6 | 查看订单来源 | 显示"商机转化: OPP-XXX" |

---

### 4.3 订单域测试用例 (20条, 关键用例摘录)

#### TC-ORD-001: 订单审批流 - 完整链路

| 项目 | 内容 |
|------|------|
| **US** | ORD-004 |
| **优先级** | P0 |
| **前置条件** | 订单处于"草稿"状态; BPM流程定义已部署; 审批人=uat_approver1 |

**测试步骤**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 订单详情页, 点击 [提交审批] | 弹出确认框 |
| 2 | 点击确认 | 订单状态变为"待审批"(1), BPM流程实例启动 |
| 3 | 切换到审批人账号(uat_approver1)登录 | - |
| 4 | 进入 "审批中心 → 我的待办" | 看到该订单审批任务 |
| 5 | 点击 [通过] | 弹出审批意见框 |
| 6 | 填写 "审批通过, 请按合同执行" + 确认 | 订单状态变为"已通过"(3) |
| 7 | 查看订单审批记录 | 显示完整审批链路: 提交人/时间→审批人/时间/意见 |

**驳回流程**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 创建新订单, 提交审批 | 状态="待审批" |
| 2 | 审批人点击 [驳回] | 需填写驳回原因 |
| 3 | 填写 "金额超出预算, 请核实" + 确认 | 订单状态="已驳回"(4) |
| 4 | 提交人收到通知(WebSocket推送) | 通知"订单ORD-XXX已被驳回" |
| 5 | 提交人修改订单后 [重新提交] | 新审批流程启动, 状态="待审批" |

**多级审批**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 创建金额=500000(>阈值)的订单, 提交 | 状态="待审批" |
| 2 | 一级审批人通过 | 状态="审批中"(2), 自动流转到二级审批人 |
| 3 | 二级审批人通过 | 状态="已通过"(3) |

---

### 4.4 财务域测试用例 (26条, 关键用例摘录)

#### TC-FIN-001: 回款逾期自动检测

| 项目 | 内容 |
|------|------|
| **US** | FIN-002 |
| **优先级** | P0 |
| **前置条件** | 回款计划: 应回款日期=昨天, 实际回款日期=null |

**测试步骤**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 手动触发逾期检测任务 | 或等待凌晨3:00定时任务 |
| 2 | 查询该回款计划 | `overdue_status` = 1 (已逾期), `overdue_days` = 1 |
| 3 | 查看回款管理列表 | 该条回款标红显示, 逾期天数=1 |
| 4 | 查看仪表板 | 逾期回款提醒出现在首页 |

---

#### TC-FIN-002: 报销审批与费用明细

| 项目 | 内容 |
|------|------|
| **US** | FIN-007 |
| **优先级** | P0 |
| **前置条件** | 员工uat_employee1登录 |

**测试步骤**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 进入 "报销管理" → [新建报销] | 报销表单 |
| 2 | 选择关联出差(可选) | 如果关联, 自动带出出差信息 |
| 3 | 添加费用明细: 交通费 300元 + 住宿费 800元 + 餐费 200元 | 总金额自动汇总=1300元 |
| 4 | 上传发票照片 (3张) | 图片上传成功, 缩略图预览 |
| 5 | 提交审批 | BPM流程启动 |
| 6 | 审批人通过 | 报销状态="已通过" |
| 7 | 财务人员确认打款 | 记录打款时间+凭证号 |

---

### 4.5 工单域测试用例 (16条, 关键用例摘录)

#### TC-WO-001: SLA超时自动升级

| 项目 | 内容 |
|------|------|
| **US** | WO-005 |
| **优先级** | P1 |
| **前置条件** | SLA规则: P1工单2小时未处理→自动升级; 工单WO-001创建于3小时前, 状态="待处理" |

**测试步骤**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 手动触发SLA检测 | 或等待定时任务 |
| 2 | 查询工单WO-001 | `sla_status` = "BREACHED", `escalated` = true |
| 3 | 查看工单处理人 | 自动升级到上级处理人 |
| 4 | 查看工单日志 | 记录"SLA超时自动升级, 原处理人: uat_support1, 升级至: uat_sales_mgr" |

---

### 4.6 营销域测试用例 (22条, 关键用例摘录)

#### TC-MKT-001: 短信群发 + 审核全流程

| 项目 | 内容 |
|------|------|
| **US** | MKT-003, MKT-004 |
| **优先级** | P1 |
| **前置条件** | SMS模板已创建并审核通过; WireMock已配置 |

**测试步骤**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 进入 "短信管理 → 模板管理" | 显示已审核通过的模板列表 |
| 2 | 新建模板 "促销通知-{客户名称}-{优惠金额}" | 模板变量高亮显示 |
| 3 | 提交模板审核 | 审批人收到审核待办 |
| 4 | 审核通过后, 进入 "短信群发" | 创建群发任务 |
| 5 | 选择模板 + 选择客户群体(10人) | 预览发送内容(含变量替换) |
| 6 | 设置发送时间 "立即发送" | - |
| 7 | 提交审核 | 审批人审核通过后执行发送 |
| 8 | 查看群发记录 | 10条发送记录, 状态="已发送" |
| 9 | 查看发送分析 | 到达率=100%, 由于WireMock全部返回成功 |

**异常场景**:
| 场景 | 操作 | 期望结果 |
|------|------|---------|
| 模板含敏感词 | 模板内容含"赌博" | 敏感词检测拦截, 提示修改 |
| 余额不足 | Mock返回余额不足 | 群发任务暂停, 提示"短信余额不足, 已发送3/10" |
| 空客户群体 | 选择0个客户 | 提示"请选择至少1个客户" |

---

### 4.7 OA域测试用例 (34条, 关键用例摘录)

#### TC-OA-001: 请假申请 - 工作日计算 + 余额扣减

| 项目 | 内容 |
|------|------|
| **US** | OA-001 |
| **优先级** | P0 |
| **前置条件** | 员工uat_employee1年假余额=5天; 法定节假日日历已配置 |

**测试步骤**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 进入 "OA → 请假申请" | 显示请假表单 + 假期余额(年假5天/事假3天/病假3天) |
| 2 | 选择请假类型 "年假" | - |
| 3 | 选择日期范围 2026-07-01(周三) ~ 2026-07-03(周五) | 系统自动计算工作日=3天(不含周末) |
| 4 | 确认请假天数显示 "3天" | 与选择的天数一致 |
| 5 | 填写请假原因 "个人旅游" | - |
| 6 | 提交审批 | BPM启动, 审批人收到待办 |
| 7 | 审批人通过 | 请假状态="已通过" |
| 8 | 再次查看假期余额 | 年假余额=2天(5-3) |

**跨周末计算**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 选择日期 2026-07-03(周五) ~ 2026-07-06(周一) | 自动识别跨越周末, 工作日=2天(周五+周一) |

**余额不足**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 选择年假, 日期范围覆盖10个工作日 | 提示"年假余额不足(当前5天, 申请10天)" |

---

#### TC-OA-002: 审批中心 - 跨域聚合

| 项目 | 内容 |
|------|------|
| **US** | OA-015 |
| **优先级** | P0 |
| **前置条件** | 审批人uat_approver1登录; 有待审批项: 1个请假+1个订单+1个报销 |

**测试步骤**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 进入 "审批中心 → 我的待办" | 显示3条待办: 请假(OA)/订单(CRM)/报销(财务) |
| 2 | 按类型筛选 "请假" | 只显示请假待办 |
| 3 | 按时间排序 | 最新待办在最上方 |
| 4 | 点击请假待办 | 跳转到请假审批详情(含申请人/类型/天数/原因) |
| 5 | 批量审批(勾选多条) | 暂不支持批量, 单个审批 |

---

### 4.8 通用测试用例 (12条)

#### TC-GEN-001: 多租户数据隔离

| 项目 | 内容 |
|------|------|
| **优先级** | P0 |
| **前置条件** | 租户A(1001)+租户B(1002), 各有客户数据 |

**测试步骤**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 租户A管理员登录, 查看客户列表 | 只显示租户A的客户 |
| 2 | 租户B管理员登录, 查看客户列表 | 只显示租户B的客户 |
| 3 | 租户A尝试通过API查询租户B的数据(修改tenant_id) | 后端拦截器自动注入tenant_id, 无法跨越 |
| 4 | 查询通用字典表(system_dict_type) | 两个租户看到各自的字典数据 |

---

#### TC-GEN-002: RBAC 权限控制

| 项目 | 内容 |
|------|------|
| **优先级** | P0 |

**测试步骤**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 销售员(uat_sales1)登录 | 只看到CRM相关菜单, 看不到系统管理菜单 |
| 2 | 普通员工(uat_employee1)登录 | 只看到OA菜单+个人中心 |
| 3 | 财务(uat_finance1)登录 | 看到财务域菜单+订单查看, 无客户编辑权限 |
| 4 | 销售员尝试访问 `/admin-api/system/user/page` | 返回403或菜单不可见 |
| 5 | 销售员在客户列表看不到 [删除] 按钮 | 按钮级别权限控制生效 |

---

#### TC-GEN-003: 国际化 - 中英文切换

| 项目 | 内容 |
|------|------|
| **优先级** | P1 |

**测试步骤**:
| 步骤 | 操作 | 期望结果 |
|:----:|------|---------|
| 1 | 当前为中文界面, 查看客户列表 | 表头: 客户名称/状态/首联系人/手机号码/所属行业 |
| 2 | 切换语言为 English | 页面刷新 |
| 3 | 查看客户列表 | 表头: Customer Name/Status/Contact/Mobile/Industry |
| 4 | 切换为阿拉伯语 | 布局方向变为RTL, 文本显示阿拉伯语 |
| 5 | 查看新增客户菜单名 | 8种语言翻译均正确显示(对照 i18n 文件) |

---

#### TC-GEN-004: 性能基准测试

| 项目 | 内容 |
|------|------|
| **优先级** | P0 |

**性能基准**:

| 接口 | 并发数 | 数据量 | 期望响应时间(P95) | 期望TPS |
|------|:------:|:------:|:-----------------:|:-------:|
| 客户分页查询 | 50 | 1000条 | <500ms | >100/s |
| 客户详情 | 50 | - | <300ms | >150/s |
| 创建客户 | 20 | - | <800ms | >25/s |
| 销售漏斗 | 10 | 100条商机 | <2s | >5/s |
| 订单审批 | 20 | - | <1s | >20/s |
| 公海领取 | 10 | - | <500ms | >20/s |

**JMeter 压测脚本模板**:
```xml
<!-- JMeter Test Plan: CRM 客户域 (片段) -->
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testname="CRM-Customer-UAT-Perf">
      <stringProp name="TestPlan.comments">客户域性能验收测试</stringProp>
    </TestPlan>
    <hashTree>
      <!-- Thread Group: 50并发, 循环10次, 持续5分钟 -->
      <ThreadGroup guiclass="ThreadGroupGui" testname="客户分页查询-50并发">
        <intProp name="ThreadGroup.num_threads">50</intProp>
        <intProp name="ThreadGroup.ramp_time">10</intProp>
        <longProp name="ThreadGroup.duration">300</longProp>
      </ThreadGroup>
      <hashTree>
        <!-- HTTP Request -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testname="POST /admin-api/crm/customer/page">
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">48080</stringProp>
          <stringProp name="HTTPSampler.path">/admin-api/crm/customer/page</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
        </HTTPSamplerProxy>
        <!-- Response Assertion: code=0 -->
        <ResponseAssertion guiclass="AssertionGui" testname="Response Assertion">
          <stringProp name="Assertion.test_field">Assertion.response_data</stringProp>
          <stringProp name="Assertion.test_type">2</stringProp>
          <stringProp name="Assertion.contains">"code":0</stringProp>
        </ResponseAssertion>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

---

## 5. 测试执行计划

### 5.1 Phase 1 (Sprint 4, 第10-12周): 工单域 + 客户域基础

| 日期 | 活动 | 参与人 | 测试用例数 |
|------|------|--------|:--------:|
| Day 1-2 | UAT环境就绪确认、测试数据部署 | DevOps + QA | - |
| Day 3-5 | 工单域全量测试 (TC-WO-001~016) | QA + 客服业务人员 | 16 |
| Day 6-10 | 客户域基础测试 (TC-CUS-001~025) | QA + 销售业务人员 | 25 |
| Day 11 | 缺陷修复验证 + 回归测试 | QA + 开发 | - |
| Day 12 | Phase 1 UAT报告 | Test Manager | - |

### 5.2 Phase 2-5 简要计划

| Phase | Sprint | 测试周期 | 测试范围 | 用例数 |
|-------|--------|---------|---------|:------:|
| Phase 2 | Sprint 6 | 5天 | 客户域全量 + 商机域 | 62 |
| Phase 3 | Sprint 8 | 5天 | 订单域 + 商机域全量 | 44 |
| Phase 4 | Sprint 9 | 5天 | 财务域 + 营销域 | 48 |
| Phase 5 | Sprint 10 | 8天 | OA域 + 审批中心 + 全量回归 | 38 |

### 5.3 缺陷管理流程

```
发现缺陷 → Jira录入 → Triage(P0/P1/P2/P3) → 开发修复 → QA复测 → 关闭
   │                                                    │
   └── P0: 阻塞验收, 24h内修复 ──────────────────────────┘
   └── P1: 影响主流程, 3天内修复
   └── P2: 非关键功能, Sprint内修复
   └── P3: 体验优化, 下Sprint修复
```

### 5.4 缺陷严重度定义

| 级别 | 定义 | 示例 | 修复SLA |
|:----:|------|------|:------:|
| P0-Blocker | 阻塞核心流程, 无法继续测试 | 创建客户报500错误; 审批流无法启动 | 24h |
| P1-Critical | 影响核心功能但可绕过 | 客户列表搜索无结果但有数据; 审批通过后状态未更新 | 3天 |
| P2-Major | 非核心功能异常 | Excel导出格式错误; 分析图表数据不准 | Sprint内 |
| P3-Minor | UI/UX体验问题 | 按钮对齐偏差; 提示文字不够清晰 | 下Sprint |

---

## 6. UAT 通过标准

### 6.1 硬性指标

| 指标 | 通过标准 | 测量方式 |
|------|---------|---------|
| P0用例通过率 | **100%** | Jira统计 |
| P1用例通过率 | **≥ 95%** | Jira统计 |
| P2用例通过率 | **≥ 90%** | Jira统计 |
| P0/P1缺陷清零 | **0个未关闭** | Jira筛选 |
| 性能基准达标 | **100%指标通过** | JMeter报告 |
| 安全扫描 | **无高危漏洞** | OWASP ZAP报告 |
| 多租户隔离 | **0泄漏** | 专项测试 |
| i18n覆盖 | **8语言覆盖率≥95%** | 翻译检查工具 |

### 6.2 UAT 签署

| 角色 | 签署内容 | 签署人 |
|------|---------|--------|
| 业务方 | 确认所有业务功能满足原型需求 | 业务验收负责人 |
| QA | 确认所有测试用例通过, 缺陷处理完毕 | QA经理 |
| 架构 | 确认架构合规, 无技术债务阻塞 | Chief Architect |
| PM | 确认整体交付质量, 批准上线 | 项目经理 |

---

## 7. UAT 交付物清单

| 交付物 | 格式 | 负责人 | 归档位置 |
|--------|------|--------|---------|
| UAT 测试计划 | .md | Test Manager | Docs/UAT/ |
| 测试用例执行记录 | .xlsx (Jira导出) | QA | Jira + Docs/UAT/ |
| 缺陷报告 | Jira导出 | QA | Jira |
| 性能测试报告 | JMeter Dashboard HTML | 性能测试工程师 | Docs/UAT/perf/ |
| 安全扫描报告 | OWASP ZAP HTML | 安全工程师 | Docs/UAT/security/ |
| 多租户隔离测试报告 | .md | QA | Docs/UAT/ |
| i18n覆盖报告 | .md | i18n专项 | Docs/UAT/ |
| UAT签署单 | PDF (签字扫描) | Test Manager | Docs/UAT/signoff/ |

---

*文档版本: 1.0*
*最后更新: 2026-03-20*
