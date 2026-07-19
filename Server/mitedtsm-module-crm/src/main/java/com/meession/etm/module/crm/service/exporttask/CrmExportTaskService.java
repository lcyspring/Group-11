package com.meession.etm.module.crm.service.exporttask;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerPageReqVO;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportDownloadTokenRespVO;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportTaskPageReqVO;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportTaskRespVO;

public interface CrmExportTaskService {
    Long createCustomerTask(CrmCustomerPageReqVO filter, Long userId);
    PageResult<CrmExportTaskRespVO> getTaskPage(CrmExportTaskPageReqVO request, Long userId);
    CrmExportTaskRespVO getTask(Long id, Long userId);
    CrmExportDownloadTokenRespVO issueDownloadToken(Long id, Long userId);
    DownloadFile download(Long id, String token, Long userId);
    int processTenantBatch();

    record DownloadFile(byte[] content, String fileName, String contentType) {
    }
}
