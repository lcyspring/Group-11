package com.meession.etm.module.crm.dal.dataobject.contract;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("crm_contract_signing")
@KeySequence("crm_contract_signing_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmContractSigningDO extends BaseDO {

    @TableId
    private Long id;
    private Long contractId;
    private Integer contractVersion;
    private Integer status;
    private Integer method;
    private LocalDateTime signedTime;
    private Long signedAttachmentId;
    private Long handlerUserId;
    private String providerCode;
    private String providerRequestId;
    private String externalSigningId;
    private String voidReason;
    private LocalDateTime voidTime;
}
