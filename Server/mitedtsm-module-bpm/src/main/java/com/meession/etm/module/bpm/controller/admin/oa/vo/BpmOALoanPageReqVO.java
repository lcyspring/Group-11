package com.meession.etm.module.bpm.controller.admin.oa.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import lombok.Data;

@Data
public class BpmOALoanPageReqVO extends PageParam {
    private String type;
    private Integer status;
    private Integer repaymentStatus;
}
