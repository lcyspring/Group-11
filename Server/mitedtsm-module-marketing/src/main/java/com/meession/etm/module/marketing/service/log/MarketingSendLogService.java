package com.meession.etm.module.marketing.service.log;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.marketing.controller.admin.log.vo.SendLogPageReqVO;
import com.meession.etm.module.marketing.controller.admin.log.vo.SendStatisticsRespVO;
import com.meession.etm.module.marketing.dal.dataobject.log.MarketingSendRecordDO;

/**
 * 营销发送记录 Service 接口
 *
 * @author MITEDTSM
 */
public interface MarketingSendLogService {

    /**
     * 获得发送记录分页
     *
     * @param pageReqVO 分页查询
     * @return 发送记录分页
     */
    PageResult<MarketingSendRecordDO> getSendLogPage(SendLogPageReqVO pageReqVO);

    /**
     * 获得发送记录详情
     *
     * @param id 记录编号
     * @return 发送记录
     */
    MarketingSendRecordDO getSendLog(Long id);

    /**
     * 获得活动发送统计
     *
     * @param campaignId 活动编号
     * @return 统计数据
     */
    SendStatisticsRespVO getStatistics(Long campaignId);

}
