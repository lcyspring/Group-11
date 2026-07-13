package com.meession.etm.module.crm.service.statistics.bo;

import lombok.Data;

/**
 * CRM 跟进客户按日去重结果。
 */
@Data
public class CrmStatisticsFollowUpCustomerByDateBO {

    /**
     * 当日首次跟进时间，使用 ISO 本地时间字符串供统计区间匹配。
     */
    private String time;

    /**
     * 客户编号。
     */
    private Long customerId;

}
