package com.meession.etm.module.crm.dal.dataobject.contract;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("crm_contract_amendment")
@KeySequence("crm_contract_amendment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmContractAmendmentDO extends BaseDO {

    @TableId
    private Long id;
    private Long contractId;
    private String no;
    private String clientRequestId;
    private String requestHash;
    private Integer baseVersion;
    private Integer targetVersion;
    private String title;
    private String reason;
    private Integer auditStatus;
    private String processInstanceId;
    private String beforeContractSnapshot;
    private String beforeProductSnapshot;
    private String afterContractSnapshot;
    private String afterProductSnapshot;
    private BigDecimal amountBefore;
    private BigDecimal amountAfter;
    private BigDecimal amountDelta;
    private Long submitterUserId;
    private LocalDateTime submitTime;
    private LocalDateTime effectiveTime;
}
