# CRM 模块代码模板

## 1. 模板说明

所有模板遵循 MITEDTSM 现有代码规范，package 路径为 `com.meession.etm.module.crm`。

模块结构:
```
mitedtsm-module-crm/
├── mitedtsm-module-crm-api/          # API接口 + DTO
│   └── src/main/java/com/meession/etm/module/crm/api/
│       ├── customer/
│       │   ├── CustomerApi.java       # 内部API接口
│       │   └── dto/
│       │       ├── CustomerDTO.java
│       │       ├── CustomerCreateReq.java
│       │       └── CustomerPageReq.java
│       ├── opportunity/...
│       ├── order/...
│       ├── finance/...
│       ├── workorder/...
│       ├── marketing/...
│       └── oa/...
└── mitedtsm-module-crm-biz/          # 实现
    └── src/main/java/com/meession/etm/module/crm/
        ├── controller/
        │   ├── admin/                 # /admin-api/crm/*
        │   │   ├── customer/
        │   │   │   └── CustomerController.java
        │   │   └── ...
        │   └── app/                   # /app-api/crm/*
        │       └── ...
        ├── service/
        │   ├── customer/
        │   │   ├── CustomerService.java
        │   │   └── CustomerServiceImpl.java
        │   └── ...
        ├── convert/
        │   ├── customer/
        │   │   └── CustomerConvert.java
        │   └── ...
        ├── dal/
        │   ├── dataobject/
        │   │   ├── customer/
        │   │   │   └── CustomerDO.java
        │   │   └── ...
        │   └── mapper/
        │       ├── customer/
        │       │   └── CustomerMapper.java
        │       └── ...
        ├── enums/
        │   └── customer/
        │       ├── CustomerStatusEnum.java
        │       └── CustomerSourceEnum.java
        ├── event/
        │   ├── OpportunityWonEvent.java
        │   └── OrderApprovedEvent.java
        ├── job/
        │   ├── CustomerSeaJob.java     # 公海自动掉入定时任务
        │   └── ReceiptOverdueJob.java  # 回款逾期检测
        └── util/
            └── OrderNoGenerator.java   # 订单编号生成器
```

---

## 2. DO 类模板

```java
package com.meession.etm.module.crm.dal.dataobject.customer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 客户 DO
 *
 * @author CRM Team
 * @since 2026-03
 */
@TableName("crm_customer")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class CustomerDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户名称 */
    @TableField("name")
    private String name;

    /** 客户状态: 1-正常 2-储备 3-淘汰 */
    @TableField("status")
    private Integer status;

    /** 客户来源: 1-线上推广 2-线下活动 3-电话营销 4-客户推荐 5-其他 */
    @TableField("source")
    private Integer source;

    /** 所属行业: 1-信息技术 2-金融 3-制造业 4-教育 5-医疗 ... */
    @TableField("industry")
    private Integer industry;

    /** 星级: 1-5 */
    @TableField("star_rating")
    private Integer starRating;

    /** 国家 */
    @TableField("country")
    private String country;

    /** 省份 */
    @TableField("province")
    private String province;

    /** 城市 */
    @TableField("city")
    private String city;

    /** 详细地址 */
    @TableField("address")
    private String address;

    /** 负责人ID (system_users.id) */
    @TableField("owner_id")
    private Long ownerId;

    /** 是否在公海: 0-否 1-是 */
    @TableField("in_sea")
    private Boolean inSea;

    /** 最后跟进时间 */
    @TableField("last_follow_up_time")
    private java.time.LocalDateTime lastFollowUpTime;

    /** 备注 */
    @TableField("notes")
    private String notes;
}
```

### 2.1 DO 类要点

- 多租户表继承 `TenantBaseDO` (含 `tenant_id`, `creator`, `updater`, `create_time`, `update_time`, `deleted`)
- 全局表(如配置)继承 `BaseDO` (不含 `tenant_id`)
- 主键 `@TableId(type = IdType.AUTO)` → MySQL AUTO_INCREMENT
- 使用 `@Accessors(chain = true)` 支持链式调用
- 逻辑删除字段 `deleted`: 0-未删除, 1-已删除
- 日期使用 `java.time.LocalDateTime`

---

## 3. Mapper 模板

```java
package com.meession.etm.module.crm.dal.mapper.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meession.etm.module.crm.dal.dataobject.customer.CustomerDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 客户 Mapper
 */
@Mapper
public interface CustomerMapper extends BaseMapper<CustomerDO> {

    /**
     * 自定义复杂查询 (MyBatis-Plus无法满足时)
     * 放在 src/main/resources/mapper/customer/CustomerMapper.xml
     */
    List<CustomerDO> selectByComplexCondition(@Param("condition") Object condition);

    /**
     * 分页查询客户 (含关联数据, 通过XML实现)
     */
    Page<CustomerDO> selectPageWithContact(Page<CustomerDO> page, @Param("req") Object req);

    /**
     * Mapper XML 默认方法示例:
     * 建议仅在复杂SQL(多表关联、聚合、子查询)时使用XML
     * 简单CRUD直接使用 BaseMapper 提供的方法
     */
    default CustomerDO selectByName(String name) {
        return selectOne(new LambdaQueryWrapper<CustomerDO>()
                .eq(CustomerDO::getName, name)
                .eq(CustomerDO::getDeleted, false));
    }

    default List<CustomerDO> selectByOwnerId(Long ownerId) {
        return selectList(new LambdaQueryWrapper<CustomerDO>()
                .eq(CustomerDO::getOwnerId, ownerId)
                .eq(CustomerDO::getDeleted, false)
                .orderByDesc(CustomerDO::getCreateTime));
    }
}
```

### 3.1 Mapper XML 模板

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.meession.etm.module.crm.dal.mapper.customer.CustomerMapper">

    <select id="selectPageWithContact" resultType="com.meession.etm.module.crm.dal.dataobject.customer.CustomerDO">
        SELECT c.*, ct.name AS first_contact_name, ct.mobile AS first_contact_mobile
        FROM crm_customer c
        LEFT JOIN crm_contact ct ON ct.customer_id = c.id AND ct.deleted = 0
        WHERE c.deleted = 0
          AND c.tenant_id = #{req.tenantId}
        <if test="req.name != null and req.name != ''">
          AND c.name LIKE CONCAT('%', #{req.name}, '%')
        </if>
        <if test="req.status != null">
          AND c.status = #{req.status}
        </if>
        ORDER BY c.create_time DESC
    </select>

</mapper>
```

---

## 4. Service 模板

### 4.1 Service 接口

```java
package com.meession.etm.module.crm.service.customer;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meession.etm.module.crm.api.customer.dto.CustomerCreateReq;
import com.meession.etm.module.crm.api.customer.dto.CustomerPageReq;
import com.meession.etm.module.crm.api.customer.dto.CustomerUpdateReq;
import com.meession.etm.module.crm.api.customer.dto.CustomerDTO;

/**
 * 客户 Service
 */
public interface CustomerService {

    /**
     * 分页查询客户
     */
    Page<CustomerDTO> page(CustomerPageReq req);

    /**
     * 查询客户详情
     */
    CustomerDTO get(Long id);

    /**
     * 创建客户
     *
     * @return 客户ID
     */
    Long create(CustomerCreateReq req);

    /**
     * 更新客户
     */
    void update(CustomerUpdateReq req);

    /**
     * 删除客户 (逻辑删除)
     */
    void delete(Long id);

    /**
     * 客户查重
     *
     * @return 重复的客户列表
     */
    List<CustomerDTO> checkDuplicate(String name, String mobile);

    /**
     * 客户转移 (变更负责人)
     */
    void transfer(Long customerId, Long newOwnerId);
}
```

### 4.2 Service 实现

```java
package com.meession.etm.module.crm.service.customer;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.api.customer.dto.*;
import com.meession.etm.module.crm.convert.customer.CustomerConvert;
import com.meession.etm.module.crm.dal.dataobject.customer.CustomerDO;
import com.meession.etm.module.crm.dal.mapper.customer.CustomerMapper;
import com.meession.etm.module.crm.enums.customer.CustomerStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户 Service 实现
 */
@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

    @Resource
    private CustomerMapper customerMapper;

    @Override
    public Page<CustomerDTO> page(CustomerPageReq req) {
        Page<CustomerDO> page = new Page<>(req.getPageNo(), req.getPageSize());
        customerMapper.selectPage(page, new LambdaQueryWrapper<CustomerDO>()
                .eq(CustomerDO::getDeleted, false)
                .eq(req.getStatus() != null, CustomerDO::getStatus, req.getStatus())
                .eq(req.getSource() != null, CustomerDO::getSource, req.getSource())
                .eq(req.getIndustry() != null, CustomerDO::getIndustry, req.getIndustry())
                .eq(req.getOwnerId() != null, CustomerDO::getOwnerId, req.getOwnerId())
                .like(StrUtil.isNotBlank(req.getName()), CustomerDO::getName, req.getName())
                .ge(req.getCreateTimeStart() != null, CustomerDO::getCreateTime, req.getCreateTimeStart())
                .le(req.getCreateTimeEnd() != null, CustomerDO::getCreateTime, req.getCreateTimeEnd())
                .orderByDesc(CustomerDO::getCreateTime));
        return CustomerConvert.INSTANCE.convertPage(page);
    }

    @Override
    public CustomerDTO get(Long id) {
        CustomerDO customer = customerMapper.selectById(id);
        if (customer == null || customer.getDeleted()) {
            throw new ServiceException("客户不存在");
        }
        return CustomerConvert.INSTANCE.convert(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomerCreateReq req) {
        // 查重校验
        checkDuplicateForCreate(req.getName(), req.getMobile());
        CustomerDO customer = CustomerConvert.INSTANCE.convert(req);
        customerMapper.insert(customer);
        return customer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CustomerUpdateReq req) {
        CustomerDO existing = customerMapper.selectById(req.getId());
        if (existing == null || existing.getDeleted()) {
            throw new ServiceException("客户不存在");
        }
        CustomerDO customer = CustomerConvert.INSTANCE.convert(req);
        customerMapper.updateById(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CustomerDO customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new ServiceException("客户不存在");
        }
        customerMapper.deleteById(id); // MyBatis-Plus 自动逻辑删除
    }

    @Override
    public List<CustomerDTO> checkDuplicate(String name, String mobile) {
        List<CustomerDO> duplicates = customerMapper.selectList(
                new LambdaQueryWrapper<CustomerDO>()
                        .eq(CustomerDO::getDeleted, false)
                        .and(w -> w.eq(CustomerDO::getName, name)
                                   .or(StrUtil.isNotBlank(mobile), q -> q.eq(/* ... */)))
        );
        return CustomerConvert.INSTANCE.convertList(duplicates);
    }

    // --- private helpers ---
    private void checkDuplicateForCreate(String name, String mobile) {
        // 具体查重逻辑
    }
}
```

---

## 5. MapStruct Convert 模板

```java
package com.meession.etm.module.crm.convert.customer;

import com.meession.etm.module.crm.api.customer.dto.CustomerCreateReq;
import com.meession.etm.module.crm.api.customer.dto.CustomerDTO;
import com.meession.etm.module.crm.api.customer.dto.CustomerUpdateReq;
import com.meession.etm.module.crm.dal.dataobject.customer.CustomerDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 客户 Convert (MapStruct)
 */
@Mapper
public interface CustomerConvert {

    CustomerConvert INSTANCE = Mappers.getMapper(CustomerConvert.class);

    /**
     * DO → DTO
     */
    CustomerDTO convert(CustomerDO source);

    /**
     * DTO 列表转换
     */
    List<CustomerDTO> convertList(List<CustomerDO> source);

    /**
     * CreateReq → DO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updater", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    CustomerDO convert(CustomerCreateReq source);

    /**
     * UpdateReq → DO (增量更新)
     */
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updater", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "inSea", ignore = true)
    void update(CustomerUpdateReq source, @MappingTarget CustomerDO target);
}
```

### 5.1 MapStruct 要点

- 使用 `Mappers.getMapper()` 单例模式 (与 Spring 注入二选一，项目使用单例模式)
- `@Mapping(target = "xxx", ignore = true)` 忽略系统自动填充字段
- Lombok + MapStruct 需 `lombok-mapstruct-binding` 依赖

---

## 6. Controller 模板

### 6.1 Web Admin Controller

```java
package com.meession.etm.module.crm.controller.admin.customer;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meession.etm.framework.common.result.CommonResult;
import com.meession.etm.framework.common.result.PageResult;
import com.meession.etm.framework.log.core.annotations.BizLog;
import com.meession.etm.module.crm.api.customer.dto.*;
import com.meession.etm.module.crm.service.customer.CustomerService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 客户管理 Controller (Admin)
 */
@RestController
@RequestMapping("/admin-api/crm/customer")
public class CustomerController {

    @Resource
    private CustomerService customerService;

    /**
     * 客户分页查询
     */
    @PostMapping("/page")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<PageResult<CustomerDTO>> page(@RequestBody CustomerPageReq req) {
        Page<CustomerDTO> page = customerService.page(req);
        return CommonResult.success(PageResult.of(page));
    }

    /**
     * 客户详情
     */
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('crm:customer:query')")
    public CommonResult<CustomerDTO> get(@RequestParam Long id) {
        return CommonResult.success(customerService.get(id));
    }

    /**
     * 创建客户
     */
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('crm:customer:create')")
    @BizLog(module = "CRM", type = "客户管理", value = "创建客户[#{#req.name}]")
    public CommonResult<Long> create(@Valid @RequestBody CustomerCreateReq req) {
        return CommonResult.success(customerService.create(req));
    }

    /**
     * 更新客户
     */
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('crm:customer:update')")
    @BizLog(module = "CRM", type = "客户管理", value = "更新客户[#{#req.id}]")
    public CommonResult<Boolean> update(@Valid @RequestBody CustomerUpdateReq req) {
        customerService.update(req);
        return CommonResult.success(true);
    }

    /**
     * 删除客户
     */
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('crm:customer:delete')")
    @BizLog(module = "CRM", type = "客户管理", value = "删除客户[#{#id}]")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        customerService.delete(id);
        return CommonResult.success(true);
    }
}
```

### 6.2 App Controller (移动端)

```java
package com.meession.etm.module.crm.controller.app.customer;

import com.meession.etm.framework.common.result.CommonResult;
import com.meession.etm.module.crm.api.customer.dto.*;
import com.meession.etm.module.crm.service.customer.CustomerService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 客户管理 Controller (App - 移动端)
 */
@RestController
@RequestMapping("/app-api/crm/customer")
public class AppCustomerController {

    @Resource
    private CustomerService customerService;

    @PostMapping("/page")
    public CommonResult<PageResult<CustomerDTO>> page(@RequestBody CustomerPageReq req) {
        // 移动端默认只查当前用户的客户
        req.setOwnerId(SecurityUtils.getLoginUserId());
        Page<CustomerDTO> page = customerService.page(req);
        return CommonResult.success(PageResult.of(page));
    }

    @GetMapping("/get")
    public CommonResult<CustomerDTO> get(@RequestParam Long id) {
        return CommonResult.success(customerService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody CustomerCreateReq req) {
        return CommonResult.success(customerService.create(req));
    }
}
```

---

## 7. DTO 模板

### 7.1 分页查询 Request

```java
package com.meession.etm.module.crm.api.customer.dto;

import com.meession.etm.framework.common.page.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 客户分页查询 Request
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerPageReq extends PageParam {

    /** 搜索关键词 (匹配名称/手机) */
    private String keyword;

    /** 客户名称 */
    private String name;

    /** 客户状态 */
    private Integer status;

    /** 客户来源 */
    private Integer source;

    /** 所属行业 */
    private Integer industry;

    /** 负责人ID */
    private Long ownerId;

    /** 创建时间开始 */
    private LocalDateTime createTimeStart;

    /** 创建时间结束 */
    private LocalDateTime createTimeEnd;
}
```

### 7.2 创建 Request

```java
package com.meession.etm.module.crm.api.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客户创建 Request
 */
@Data
public class CustomerCreateReq {

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称最长100个字符")
    private String name;

    private Integer status;

    private Integer source;

    private Integer industry;

    private Integer starRating;

    private String country;

    private String province;

    private String city;

    private String address;

    private Long ownerId;

    private String notes;
}
```

### 7.3 响应 DTO

```java
package com.meession.etm.module.crm.api.customer.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户 DTO (响应)
 */
@Data
public class CustomerDTO {

    private Long id;
    private String name;
    private Integer status;
    private String statusName;
    private Integer source;
    private String sourceName;
    private Integer industry;
    private String industryName;
    private Integer starRating;
    private String country;
    private String province;
    private String city;
    private String address;
    private Long ownerId;
    private String ownerName;
    private Boolean inSea;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

---

## 8. 枚举模板

```java
package com.meession.etm.module.crm.enums.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户状态枚举
 */
@Getter
@AllArgsConstructor
public enum CustomerStatusEnum {

    NORMAL(1, "正常"),
    RESERVE(2, "储备"),
    ELIMINATED(3, "淘汰");

    private final Integer code;
    private final String name;

    public static CustomerStatusEnum of(Integer code) {
        for (CustomerStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
```

---

## 9. 领域事件模板

```java
package com.meession.etm.module.crm.event;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

/**
 * 商机成交事件
 * 发布后: Team 5 (订单域) 监听并自动创建订单
 */
@Getter
@ToString
public class OpportunityWonEvent extends ApplicationEvent {

    private final Long opportunityId;
    private final Long customerId;
    private final Long totalAmount;

    public OpportunityWonEvent(Object source, Long opportunityId, Long customerId, Long totalAmount) {
        super(source);
        this.opportunityId = opportunityId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }
}
```

### 9.1 事件监听器

```java
package com.meession.etm.module.crm.service.order;

import com.meession.etm.module.crm.event.OpportunityWonEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 订单域事件监听器
 */
@Slf4j
@Component
public class OrderEventListener {

    @EventListener
    public void onOpportunityWon(OpportunityWonEvent event) {
        log.info("收到商机成交事件, opportunityId={}, customerId={}", 
                 event.getOpportunityId(), event.getCustomerId());
        // 自动创建订单
        // orderService.createFromOpportunity(event);
    }
}
```

---

## 10. 定时任务模板

```java
package com.meession.etm.module.crm.job;

import com.meession.etm.module.crm.service.customer.CustomerSeaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 公海定时任务: 自动将未跟进客户移入公海
 */
@Slf4j
@Component
public class CustomerSeaJob {

    @Resource
    private CustomerSeaService customerSeaService;

    /**
     * 每天凌晨 2:00 执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoMoveToSea() {
        log.info("开始执行公海自动掉入任务");
        try {
            int count = customerSeaService.autoMoveToSea();
            log.info("公海自动掉入任务完成, 处理{}条", count);
        } catch (Exception e) {
            log.error("公海自动掉入任务失败", e);
        }
    }
}
```

---

## 11. 数据库迁移 SQL 模板

```sql
-- ==========================================
-- CRM 客户域 DDL
-- 文件: InstallPackage/database/new/crm/001-crm-customer.sql
-- 版本: 2026.01-SNAPSHOT
-- 日期: 2026-03-26
-- ==========================================

CREATE TABLE `crm_customer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '客户名称',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '客户状态: 1-正常 2-储备 3-淘汰',
    `source` TINYINT DEFAULT NULL COMMENT '客户来源: 1-线上推广 2-线下活动 3-电话营销 4-客户推荐 5-其他',
    `industry` TINYINT DEFAULT NULL COMMENT '所属行业',
    `star_rating` TINYINT DEFAULT 1 COMMENT '星级: 1-5',
    `country` VARCHAR(50) DEFAULT NULL COMMENT '国家',
    `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
    `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
    `owner_id` BIGINT DEFAULT NULL COMMENT '负责人ID',
    `in_sea` TINYINT NOT NULL DEFAULT 0 COMMENT '是否在公海: 0-否 1-是',
    `last_follow_up_time` DATETIME DEFAULT NULL COMMENT '最后跟进时间',
    `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_owner_id` (`owner_id`),
    INDEX `idx_name` (`name`),
    INDEX `idx_status` (`status`),
    INDEX `idx_in_sea` (`in_sea`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM客户表';

-- 回滚脚本 (如需回滚请执行)
-- DROP TABLE IF EXISTS `crm_customer`;
```

### 11.1 SQL 模板要点

- 所有表使用 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`
- 主键统一 `BIGINT AUTO_INCREMENT`
- 多租户表必须含 `tenant_id` + `INDEX idx_tenant_id`
- 系统字段标准: `creator`, `create_time`, `updater`, `update_time`, `deleted`
- 注释使用中文
- 每个DDL文件末尾附回滚脚本

---

## 12. 菜单与权限 SQL 模板

```sql
-- ==========================================
-- CRM 菜单与权限数据
-- 文件: InstallPackage/database/new/crm/002-crm-menu.sql
-- ==========================================

-- 一级菜单: CRM
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `tenant_id`)
VALUES ('CRM', NULL, 1, 100, 0, '/crm', 'ep:management', '', 0, 1);

SET @crm_menu_id = LAST_INSERT_ID();

-- 二级菜单: 客户管理
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `tenant_id`)
VALUES ('客户管理', NULL, 1, 1, @crm_menu_id, 'customer', 'ep:user', 'views/crm/customer/index', 0, 1);

SET @customer_menu_id = LAST_INSERT_ID();

-- 按钮权限: 客户创建
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `component`, `status`, `tenant_id`)
VALUES ('创建客户', 'crm:customer:create', 2, 1, @customer_menu_id, '', '', 0, 1);

-- 菜单i18n (中文)
INSERT INTO `system_menu_i18n` (`menu_id`, `lang`, `name`)
VALUES (@crm_menu_id, 'zh-CN', 'CRM'),
       (@customer_menu_id, 'zh-CN', '客户管理');

-- 菜单i18n (英文)
INSERT INTO `system_menu_i18n` (`menu_id`, `lang`, `name`)
VALUES (@crm_menu_id, 'en-US', 'CRM'),
       (@customer_menu_id, 'en-US', 'Customer Management');
```
