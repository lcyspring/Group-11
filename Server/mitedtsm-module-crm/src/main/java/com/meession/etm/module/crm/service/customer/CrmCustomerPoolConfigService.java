package com.meession.etm.module.crm.service.customer;

import com.meession.etm.module.crm.controller.admin.customer.vo.poolconfig.CrmCustomerPoolConfigSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolConfigDO;

import jakarta.validation.Valid;

/**
 * 客户公海配置 Service 接口
 *
 * @author Wanwan
 */
public interface CrmCustomerPoolConfigService {

    /**
     * 获得客户公海配置
     *
     * @return 客户公海配置
     */
    CrmCustomerPoolConfigDO getCustomerPoolConfig();

    /**
     * 获得客户自动回收单批数量的 YAML 安全上限。
     */
    int getAutoPoolMaxBatchSize();

    /**
     * 保存客户公海配置
     *
     * @param saveReqVO 更新信息
     */
    void saveCustomerPoolConfig(@Valid CrmCustomerPoolConfigSaveReqVO saveReqVO);

}
