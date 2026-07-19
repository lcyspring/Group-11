package com.meession.etm.module.crm.service.customer;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.customer.vo.garbage.CrmCustomerGarbagePageReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.garbage.CrmCustomerGarbagePutReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import jakarta.validation.Valid;

public interface CrmCustomerGarbageService {

    PageResult<CrmCustomerDO> getGarbagePage(@Valid CrmCustomerGarbagePageReqVO pageReqVO, Long userId);

    void putCustomerGarbage(@Valid CrmCustomerGarbagePutReqVO reqVO, Long userId);

    void restoreCustomerToPublicPool(Long customerId, Long userId);

    void permanentlyDeleteGarbageCustomer(Long customerId, Long userId);

    int autoPutCustomerGarbage();
}
