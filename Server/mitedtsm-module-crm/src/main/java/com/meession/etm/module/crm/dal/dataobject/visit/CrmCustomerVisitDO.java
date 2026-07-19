package com.meession.etm.module.crm.dal.dataobject.visit;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.framework.mybatis.core.type.LongListTypeHandler;
import com.meession.etm.framework.mybatis.core.type.StringListTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@TableName(value = "crm_customer_visit", autoResultMap = true)
@KeySequence("crm_customer_visit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmCustomerVisitDO extends BaseDO {
    @TableId private Long id;
    private Long applicantUserId;
    private Long customerId;
    private Long contactId;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private String location;
    private String purpose;
    @TableField(typeHandler = LongListTypeHandler.class) private List<Long> participantUserIds;
    @TableField(typeHandler = StringListTypeHandler.class) private List<String> attachmentUrls;
    private Integer auditStatus;
    private String processInstanceId;
    private LocalDateTime approvalTime;
    private Integer resultStatus;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private String resultContent;
    private LocalDateTime nextContactTime;
    @TableField(typeHandler = StringListTypeHandler.class) private List<String> resultAttachmentUrls;
    private Long followUpRecordId;
}
