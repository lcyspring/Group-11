package com.meession.etm.module.crm.dal.dataobject.business;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("crm_business_status_record")
@KeySequence("crm_business_status_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmBusinessStatusRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long businessId;

    private Long oldStatusTypeId;

    private Long oldStatusId;

    private Long newStatusTypeId;

    private Long newStatusId;

    private Integer oldEndStatus;

    private Integer newEndStatus;

    private Long operatorId;

    private String remark;

}