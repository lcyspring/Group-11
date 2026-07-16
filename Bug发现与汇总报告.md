# Bug发现与汇总报告

## 报告信息

| 项目 | 内容 |
|------|------|
| **报告日期** | 2026-07-16 |
| **开发功能** | 1. 公海自动掉入任务、公海客户领取接口；2. 联系人管理接口、客户转移/归属变更接口 |
| **代码分支** | `xla` |
| **报告状态** | 部分修复 |

---

## 一、Bug列表

### 🔴 严重级别（Critical）

#### Bug #1: 客户放入公海时未更新公海状态字段

**状态**: ✅ **已解决**

**位置**: `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/CrmCustomerServiceImpl.java:503-518`

**问题描述**: `putCustomerPool(CrmCustomerDO customer)` 方法仅设置了 `ownerUserId = null`，但**没有更新公海相关字段**（`pool_status`、`pool_time`、`pool_reason`、`pool_rule_id`）。

**影响**:
- 客户放入公海后，`pool_status` 仍为 0（非公海状态），导致公海列表查询无法正确识别该客户
- `pool_time` 为空，无法追踪客户进入公海的时间
- `pool_reason` 和 `pool_rule_id` 为空，无法记录进入公海的原因和触发规则

**修复方式**:
1. 修改方法签名，增加 `poolReason` 和 `poolRuleId` 参数
2. 在方法中添加 `customerMapper.updatePoolStatus()` 调用，更新公海状态字段
3. 更新两处调用：手动放入公海传 `"手动放入公海"`，自动回收传 `"自动回收-超时"`

**修复时间**: 2026-07-16

---

### 🔴 严重级别（Critical）

#### Bug #2: 客户领取公海时未更新公海状态字段

**状态**: ✅ **已解决**

**位置**: `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/CrmCustomerServiceImpl.java:403-470`

**问题描述**: `receiveCustomer` 方法仅更新了 `ownerUserId` 和 `ownerTime`，但**没有更新公海相关字段**：
- `pool_status` 未重置为 0（应表示已离开公海）
- `last_receive_time` 未记录领取时间
- `receive_freeze_end_time` 未根据规则设置冻结截止时间

**影响**:
- 客户领取后仍可能被识别为公海客户
- 冻结期规则无法生效（`receive_freeze_end_time` 为空）
- 无法统计客户领取频率

**修复方式**:
1. 使用 `customerMapper.updateReceiveInfo()` 方法替代原有的 `updateBatch`，一次性更新负责人和公海状态字段
2. 新增 `getReceiveFreezeDays()` 方法，从领取规则配置中获取冻结期天数
3. 添加联系人负责人同步更新：`contactService.updateOwnerUserIdByCustomerId()`

**修复时间**: 2026-07-16

---

### 🔴 严重级别（Critical）

#### Bug #3: 公海自动掉入任务未记录操作日志

**状态**: ✅ **已解决**

**位置**: `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/CrmCustomerServiceImpl.java:482-501`

**问题描述**: `autoPutCustomerPool` 方法使用 `try-catch` 包裹单个客户处理，但**没有记录任何公海操作日志**，包括：
- 未调用 `customerMapper.updatePoolStatus()` 更新公海状态
- 未写入 `crm_customer_pool_log` 操作日志表

**影响**:
- 无法追踪自动掉入公海的客户记录
- 无法审计定时任务的执行结果
- 客户公海状态数据不一致

**修复方式**:
1. 在 `LogRecordConstants.java` 中添加自动回收日志常量
2. 在 `CrmCustomerServiceImpl.java` 中新增 `autoPutCustomerPoolLog()` 方法，使用 `@LogRecord` 注解记录操作日志
3. 在 `autoPutCustomerPool` 方法中调用日志记录方法

**修复时间**: 2026-07-16

---

### 🟡 中等级别（Medium）

#### Bug #4: 联系人管理接口权限注解错误

**状态**: ⚠️ **待修复**

**位置**: `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/CrmContactServiceImpl.java:198-216`

**问题描述**: `updateOwnerUserIdByCustomerId` 方法的权限注解使用了错误的业务类型：

```java
@CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#customerId", level = CrmPermissionLevelEnum.OWNER)
public void updateOwnerUserIdByCustomerId(Long customerId, Long ownerUserId) {
    // 操作的是联系人数据，但校验的是客户权限
}
```

**影响**:
- 权限校验逻辑错误，可能导致：
  - 有权限修改客户的用户可以修改不属于自己的联系人
  - 无权修改客户但有权修改联系人的用户无法操作

---

### 🟡 中等级别（Medium）

#### Bug #5: 客户转移时缺少关联数据的权限校验

**状态**: ⚠️ **待修复**

**位置**: `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/CrmCustomerServiceImpl.java:251-267`

**问题描述**: `transfer` 方法在转移关联数据（联系人、商机、合同）时，**没有校验当前用户对这些关联数据的权限**：

```java
private void transfer(CrmCustomerTransferReqVO reqVO, Long userId) {
    if (reqVO.getToBizTypes().contains(CrmBizTypeEnum.CRM_CONTACT.getType())) {
        List<CrmContactDO> contactList = contactService.getContactListByCustomerIdOwnerUserId(reqVO.getId(), userId);
        contactList.forEach(item -> contactService.transferContact(...)); // 可能抛出权限异常
    }
}
```

**影响**:
- 如果关联数据的负责人与客户负责人不一致，会导致权限校验失败
- 批量转移时部分成功部分失败，导致数据不一致

---

### 🟡 中等级别（Medium）

#### Bug #6: 公海自动掉入查询逻辑错误

**状态**: ⚠️ **待修复**

**位置**: `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/dal/mysql/customer/CrmCustomerMapper.java:144-161`

**问题描述**: `selectListByAutoPool` 方法的查询条件存在逻辑缺陷：

```java
query.and(q -> {
    q.lt(CrmCustomerDO::getOwnerTime, dealExpireTime)
    .or(w -> w.lt(CrmCustomerDO::getOwnerTime, contactExpireTime)
            .and(p -> p.lt(CrmCustomerDO::getContactLastTime, contactExpireTime)
                    .or().isNull(CrmCustomerDO::getContactLastTime)));
});
```

**问题**:
- 条件要求 `ownerTime < contactExpireTime`，但实际上应该是 `contactLastTime < contactExpireTime`（跟进超时）或客户从未被跟进过
- 客户分配时间（`ownerTime`）和跟进时间（`contactLastTime`）是两个不同的概念

**影响**:
- 部分应该进入公海的客户无法被识别
- 部分不该进入公海的客户可能被错误回收

---

### 🟢 轻微级别（Low）

#### Bug #7: 联系人转移时缺少负责人校验

**状态**: ⚠️ **待修复**

**位置**: `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/CrmContactServiceImpl.java:184-196`

**问题描述**: `transferContact` 方法没有校验新负责人是否存在以及是否到达客户/联系人数量上限。

**影响**:
- 如果传入无效的用户ID，会导致联系人负责人设置为无效值

---

### 🟢 轻微级别（Low）

#### Bug #8: 客户领取时缺少联系人负责人同步

**状态**: ✅ **已解决**（在 Bug #2 修复中一并解决）

**位置**: `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/CrmCustomerServiceImpl.java:460`

**问题描述**: 客户领取后，关联联系人的负责人仍为空。

**修复方式**: 在 `receiveCustomer` 方法中添加了联系人负责人同步更新：
```java
contactService.updateOwnerUserIdByCustomerId(customer.getId(), ownerUserId);
```

**修复时间**: 2026-07-16

---

## 二、已解决的Bug汇总

| Bug编号 | 问题描述 | 修复方式 | 修复时间 |
|---------|----------|----------|----------|
| Bug #1 | 客户放入公海时未更新公海状态字段 | 添加 `updatePoolStatus` 调用，更新 pool_status、pool_time、pool_reason、pool_rule_id | 2026-07-16 |
| Bug #2 | 客户领取公海时未更新公海状态字段 | 使用 `updateReceiveInfo` 更新公海状态，添加冻结期计算和联系人同步 | 2026-07-16 |
| Bug #3 | 公海自动掉入任务未记录操作日志 | 添加 `@LogRecord` 日志注解和日志记录方法 | 2026-07-16 |
| Bug #8 | 客户领取时缺少联系人负责人同步 | 在 `receiveCustomer` 中添加联系人负责人同步更新 | 2026-07-16 |

---

## 三、待修复Bug汇总

| Bug编号 | 优先级 | 问题描述 |
|---------|--------|----------|
| Bug #6 | P1 | 公海自动掉入查询逻辑错误 |
| Bug #4 | P2 | 联系人管理接口权限注解错误 |
| Bug #5 | P2 | 客户转移时缺少关联数据的权限校验 |
| Bug #7 | P2 | 联系人转移时缺少负责人校验 |

---

## 四、风险评估

| 风险项 | 风险等级 | 当前状态 | 说明 |
|--------|----------|----------|------|
| 公海状态数据不一致 | **高** | ✅ 已修复 | Bug #1、#2 已修复，公海状态字段更新正常 |
| 自动掉入任务无效 | **高** | ✅ 已修复 | Bug #3、#8 已修复，日志记录和联系人同步正常 |
| 公海查询逻辑错误 | **中** | ⚠️ 待修复 | Bug #6 可能导致部分客户无法正确回收 |
| 权限校验漏洞 | **中** | ⚠️ 待修复 | Bug #4、#5 可能导致越权操作或操作失败 |
| 数据完整性问题 | **低** | ⚠️ 待修复 | Bug #7 可能导致联系人负责人设置为无效值 |

---

## 五、结论

**已修复**: 4个Bug（Bug #1、#2、#3、#8）
**待修复**: 4个Bug（Bug #4、#5、#6、#7）

建议优先修复 Bug #6（公海查询逻辑错误），因为它会影响自动回收任务的正确性。其余待修复Bug可以在后续迭代中逐步解决。