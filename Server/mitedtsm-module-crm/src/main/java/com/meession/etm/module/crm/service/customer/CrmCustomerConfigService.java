package com.meession.etm.module.crm.service.customer;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.customer.vo.config.CrmCustomerConfigSaveReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.config.CrmCustomerConfigRespVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerConfigDO;
import jakarta.validation.Valid;

import java.util.List;

public interface CrmCustomerConfigService {

    Long createConfig(@Valid CrmCustomerConfigSaveReqVO reqVO);

    void updateConfig(@Valid CrmCustomerConfigSaveReqVO reqVO);

    void deleteConfig(Long id);

    CrmCustomerConfigDO getConfig(Long id);

    List<CrmCustomerConfigDO> getConfigListByType(String configType);

    List<CrmCustomerConfigRespVO> getConfigListByTypeVo(String configType);

    PageResult<CrmCustomerConfigRespVO> getConfigPage(String configType);

    void validateConfig(Long id);

}