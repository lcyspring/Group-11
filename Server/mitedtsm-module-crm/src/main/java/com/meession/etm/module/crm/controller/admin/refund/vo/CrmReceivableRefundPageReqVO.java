package com.meession.etm.module.crm.controller.admin.refund.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import com.meession.etm.module.crm.enums.refund.CrmReceivableRefundTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@EqualsAndHashCode(callSuper = true)
public class CrmReceivableRefundPageReqVO extends PageParam {

    private String no;
    private Long receivableId;
    private Long customerId;
    private Long contractId;
    @InEnum(CrmReceivableRefundTypeEnum.class)
    private Integer type;
    @InEnum(CrmAuditStatusEnum.class)
    private Integer auditStatus;
    @InEnum(CrmSceneTypeEnum.class)
    private Integer sceneType;
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] refundTime;
}
