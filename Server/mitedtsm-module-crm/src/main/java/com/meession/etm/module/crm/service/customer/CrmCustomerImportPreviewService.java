package com.meession.etm.module.crm.service.customer;

import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportPreviewReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportPreviewRespVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportRespVO;

import java.io.IOException;

public interface CrmCustomerImportPreviewService {

    CrmCustomerImportPreviewRespVO createPreview(CrmCustomerImportPreviewReqVO request, Long userId)
            throws IOException;

    CrmCustomerImportPreviewRespVO getPreview(Long id, Long userId);

    CrmCustomerImportRespVO confirmPreview(Long id, Long userId);
}
