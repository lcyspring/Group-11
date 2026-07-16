package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkReportUpdateReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAWorkReportDO;

public interface BpmOAWorkReportService {

    Long createWorkReport(Long userId, BpmOAWorkReportCreateReqVO createReqVO);

    void updateWorkReport(BpmOAWorkReportUpdateReqVO updateReqVO);

    void deleteWorkReport(Long id);

    BpmOAWorkReportDO getWorkReport(Long id);

    PageResult<BpmOAWorkReportDO> getWorkReportPage(Long userId, BpmOAWorkReportPageReqVO pageReqVO);

}