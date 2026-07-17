package com.meession.etm.module.crm.dal.dataobject.customer;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("crm_customer_import_preview")
@KeySequence("crm_customer_import_preview_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmCustomerImportPreviewDO extends BaseDO {

    @TableId
    private Long id;
    private String fileName;
    private String fieldMapping;
    private Boolean updateSupport;
    private Long ownerUserId;
    private Integer status;
    private Integer totalCount;
    private Integer createCount;
    private Integer updateCount;
    private Integer failureCount;
    private String rowsSnapshot;
    private String resultSnapshot;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
}
