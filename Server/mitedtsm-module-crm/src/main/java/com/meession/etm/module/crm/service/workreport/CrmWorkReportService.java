package com.meession.etm.module.crm.service.workreport;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.workreport.vo.*;
import com.meession.etm.module.crm.dal.dataobject.workreport.CrmWorkReportDO;

public interface CrmWorkReportService {
    Long create(Long userId, CrmWorkReportSaveReqVO req);
    void update(Long userId, CrmWorkReportSaveReqVO req);
    void submit(Long userId, Long id);
    void delete(Long userId, Long id);
    CrmWorkReportDO get(Long userId, Long id);
    PageResult<CrmWorkReportDO> page(Long userId, CrmWorkReportPageReqVO req);
}
