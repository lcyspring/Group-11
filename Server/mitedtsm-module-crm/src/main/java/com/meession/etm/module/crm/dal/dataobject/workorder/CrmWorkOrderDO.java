package com.meession.etm.module.crm.dal.dataobject.workorder;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@TableName(value = "crm_work_order", autoResultMap = true)
@KeySequence("crm_work_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmWorkOrderDO extends BaseDO {

    @TableId
    private Long id;
    private String no;
    private String title;
    private Integer type;
    private Integer priority;
    private Integer status;
    private Long customerId;
    private Integer sourceType;
    private Long sourceId;
    private BigDecimal serviceLatitude;
    private BigDecimal serviceLongitude;
    private Integer geofenceRadiusMeters;
    private Boolean checkInRequired;
    private Long groupId;
    private Long handlerUserId;
    private Integer dispatchMode;
    private LocalDateTime assignTime;
    private String description;
    private String solution;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachmentUrls;
    private LocalDateTime processTime;
    private LocalDateTime completeTime;
    private String returnReason;
}
