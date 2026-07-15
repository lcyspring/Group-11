package com.meession.etm.module.crm.controller.admin.reimbursement.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmSceneTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Data
@EqualsAndHashCode(callSuper = true)
public class CrmReimbursementPageReqVO extends PageParam {
    private String no;
    private Long applicantUserId;
    private Long customerId;
    private Long contractId;
    @InEnum(CrmAuditStatusEnum.class)
    private Integer auditStatus;
    @InEnum(CrmSceneTypeEnum.class)
    private Integer sceneType;
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] expenseDate;
}
