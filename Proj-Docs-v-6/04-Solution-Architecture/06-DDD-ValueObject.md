# DDD 值对象设计 - 密讯ETM系统 (mitedtsm)

---

## 文档信息

| 字段 | 内容 |
|------|------|
| 项目名称 | 密讯ETM企业管理系统 (mitedtsm) |
| 文档版本 | v1.0 |
| 创建日期 | 2026-06-25 |
| 负责人 | 架构团队 |
| 状态 | 已发布 |

---

## 一、值对象设计原则

1. **不可变性**：值对象创建后不可修改，所有修改操作返回新实例
2. **相等性**：基于属性值判断相等，而非标识符
3. **自验证**：构造函数中完成所有校验，确保不会创建无效值对象
4. **可替换性**：通过整体替换而非修改属性

---

## 二、核心值对象设计

### 2.1 Password（密码）

```java
package com.meession.etm.module.system.enums;

import java.util.regex.Pattern;

public enum UserStatus {
    ACTIVE(0, "正常"),
    DISABLED(1, "禁用"),
    LOCKED(2, "锁定");

    private final Integer code;
    private final String desc;

    UserStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}
```

密码复杂度校验通常在 Service 层实现：

```java
// 密码复杂度校验（8位以上，含大小写+数字+特殊字符）
private static final Pattern PASSWORD_PATTERN =
    Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$");

public static void validateRawPassword(String rawPassword) {
    if (rawPassword == null || !PASSWORD_PATTERN.matcher(rawPassword).matches()) {
        throw new BusinessException("密码必须8位以上，包含大小写字母、数字和特殊字符");
    }
}
```

### 2.2 CustomerLevel（客户等级）

```java
package com.meession.etm.module.crm.enums;

import lombok.Getter;

@Getter
public enum CustomerLevel {
    VIP(5, "VIP客户"),
    GOLD(4, "金牌客户"),
    SILVER(3, "银牌客户"),
    BRONZE(2, "铜牌客户"),
    NORMAL(1, "普通客户");

    private final int priority;
    private final String description;

    CustomerLevel(int priority, String description) {
        this.priority = priority;
        this.description = description;
    }

    public static CustomerLevel fromCode(Integer code) {
        for (CustomerLevel level : values()) {
            if (level.priority == code) return level;
        }
        return NORMAL;
    }
}
```

### 2.3 SalesStage（商机阶段）

```java
package com.meession.etm.module.crm.enums;

import lombok.Getter;

@Getter
public enum SalesStage {
    LEAD(1, "初步接洽"),
    QUALIFIED(2, "需求分析"),
    PROPOSAL(3, "方案报价"),
    NEGOTIATION(4, "商务谈判"),
    WON(5, "赢单"),
    LOST(6, "丢单");

    private final int order;
    private final String description;

    SalesStage(int order, String description) {
        this.order = order;
        this.description = description;
    }

    public boolean canAdvanceTo(SalesStage next) {
        return next.order > this.order && this != LOST && this != WON;
    }
}
```

### 2.4 OrderStatus（订单状态）

```java
package com.meession.etm.module.crm.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    DRAFT(0, "草稿"),
    PENDING(1, "待支付"),
    PAID(2, "已支付"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OrderStatus fromCode(Integer code) {
        for (OrderStatus status : values()) {
            if (status.code == code) return status;
        }
        return DRAFT;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
```

### 2.5 PayChannel / PayStatus（支付枚举）

```java
package com.meession.etm.module.pay.enums;

@Getter
public enum PayChannel {
    WECHAT("微信支付"),
    ALIPAY("支付宝");

    private final String description;
    PayChannel(String description) { this.description = description; }
}

@Getter
public enum PayStatus {
    PENDING(0, "待支付"),
    PAYING(1, "支付中"),
    SUCCESS(2, "支付成功"),
    FAILED(3, "支付失败"),
    CLOSED(4, "已关闭");

    private final int code;
    private final String description;
    PayStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == CLOSED;
    }
}
```

### 2.6 WorkOrderStatus（工单状态）

```java
package com.meession.etm.module.mes.enums;

@Getter
public enum WorkOrderStatus {
    CREATED(0, "已创建"),
    RELEASED(1, "已下达"),
    IN_PROGRESS(2, "进行中"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;
    WorkOrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

### 2.7 ApprovalAction / TaskStatus（审批状态）

```java
package com.meession.etm.module.bpm.enums;

@Getter
public enum ApprovalAction {
    APPROVED("通过"),
    REJECTED("驳回"),
    DELEGATED("转办");

    private final String description;
    ApprovalAction(String description) { this.description = description; }
}

@Getter
public enum TaskStatus {
    PENDING(0, "待处理"),
    COMPLETED(1, "已完成"),
    CANCELLED(2, "已取消");

    private final int code;
    private final String description;
    TaskStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

### 2.8 Address（地址 - 值对象）

```java
package com.meession.etm.framework.util;

import lombok.Value;

@Value  // Lombok: 所有字段 private final, equals/hashCode/toString
public class Address {
    String province;
    String city;
    String district;
    String detail;

    public String fullAddress() {
        StringBuilder sb = new StringBuilder();
        if (province != null) sb.append(province);
        if (city != null) sb.append(city);
        if (district != null) sb.append(district);
        if (detail != null) sb.append(detail);
        return sb.toString();
    }
}
```

### 2.9 DateRange（日期范围）

```java
package com.meession.etm.framework.util;

import lombok.Value;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Value
public class DateRange {
    LocalDate startDate;
    LocalDate endDate;

    public DateRange {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public long daysBetween() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
}
```

---

## 三、值对象与数据库映射

在 mitedtsm 项目中，值对象的数据通常直接存储在 DO 类的字段中：

| 值对象 | DO 类 | 数据库字段 | 存储方式 |
|--------|-------|-----------|---------|
| CustomerLevel | CustomerDO | `level` INT | 存储 priority 值 (1-5) |
| SalesStage | OpportunityDO | `stage` INT | 存储 order 值 (1-6) |
| OrderStatus | OrderDO | `status` INT | 存储 code 值 (0-4) |
| PayChannel | PaymentOrderDO | `channel` VARCHAR | 存储枚举名 (WECHAT/ALIPAY) |
| PayStatus | PaymentOrderDO | `status` INT | 存储 code 值 (0-4) |
| Address | CustomerDO | province/city/detailAddress | 分字段存储 |
| DateRange | CampaignDO 等 | start_time/end_time | 分字段存储 |
| WorkOrderStatus | WorkOrderDO | `status` INT | 存储 code 值 (0-4) |

**MyBatis-Plus 枚举自动映射**：

```yaml
# application.yaml
mybatis-plus:
  type-enums-package: com.meession.etm.module.**.enums
```

```java
// 在 DO 类中直接使用枚举类型
@TableName("crm_order")
public class OrderDO extends TenantBaseDO {
    private OrderStatus status;  // 自动映射为 INT
}
```

---

> 下一步：基于值对象设计，进行领域服务设计。
