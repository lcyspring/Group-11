package com.meession.etm.module.bpm.dal.dataobject.oa;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("bpm_oa_customer_visit")
@KeySequence("bpm_oa_customer_visit_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmOACustomerVisitDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private Long customerId;

    private String customerName;

    private String contactPerson;

    private String contactPhone;

    private String visitAddress;

    private LocalDateTime visitTime;

    private String purpose;

    private String content;

    private Integer status;

    private String processInstanceId;

}