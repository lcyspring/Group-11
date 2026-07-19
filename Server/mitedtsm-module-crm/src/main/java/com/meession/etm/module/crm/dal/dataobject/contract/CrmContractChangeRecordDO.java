package com.meession.etm.module.crm.dal.dataobject.contract;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("crm_contract_change_record")
@KeySequence("crm_contract_change_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmContractChangeRecordDO extends BaseDO {

    @TableId
    private Long id;
    private Long contractId;
    private Integer sequenceNo;
    private Integer contractVersion;
    private Integer actionType;
    private Long operatorUserId;
    private String reason;
    private String contractSnapshot;
    private String productSnapshot;
    private LocalDateTime actionTime;
}
