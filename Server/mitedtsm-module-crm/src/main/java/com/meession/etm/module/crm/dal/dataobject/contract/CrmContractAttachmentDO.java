package com.meession.etm.module.crm.dal.dataobject.contract;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("crm_contract_attachment")
@KeySequence("crm_contract_attachment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmContractAttachmentDO extends BaseDO {

    @TableId
    private Long id;
    private Long contractId;
    private Long amendmentId;
    private Integer contractVersion;
    private Integer category;
    private String fileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private Boolean immutable;
    private Long uploaderUserId;
}
