package com.meession.etm.module.crm.service.customer;

import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.customer.vo.poolconfig.CrmCustomerPoolConfigSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolConfigDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerPoolConfigMapper;
import com.meession.etm.module.crm.framework.pool.CrmPoolPolicyProperties;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_POOL_BATCH_SIZE_EXCEEDS_POLICY;
import static com.meession.etm.module.crm.enums.LogRecordConstants.*;

/**
 * 客户公海配置 Service 实现类
 *
 * @author Wanwan
 */
@Service
@Validated
public class CrmCustomerPoolConfigServiceImpl implements CrmCustomerPoolConfigService {

    @Resource
    private CrmCustomerPoolConfigMapper customerPoolConfigMapper;
    @Resource
    private CrmPoolPolicyProperties poolPolicyProperties;

    @Override
    public CrmCustomerPoolConfigDO getCustomerPoolConfig() {
        CrmCustomerPoolConfigDO config = customerPoolConfigMapper.selectOne();
        if (config == null) {
            config = new CrmCustomerPoolConfigDO();
        }
        CrmPoolPolicyProperties.Customer defaults = poolPolicyProperties.getCustomer();
        if (config.getEnabled() == null) config.setEnabled(defaults.isEnabled());
        if (config.getContactExpireDays() == null) config.setContactExpireDays(defaults.getContactExpireDays());
        if (config.getDealExpireDays() == null) config.setDealExpireDays(defaults.getDealExpireDays());
        if (config.getNotifyEnabled() == null) config.setNotifyEnabled(defaults.isNotifyEnabled());
        if (config.getNotifyDays() == null) config.setNotifyDays(defaults.getNotifyDays());
        if (config.getDailyClaimLimit() == null) config.setDailyClaimLimit(defaults.getDailyClaimLimit());
        if (config.getRepeatClaimCooldownDays() == null) {
            config.setRepeatClaimCooldownDays(defaults.getRepeatClaimCooldownDays());
        }
        if (config.getHighValueLevelThreshold() == null) {
            config.setHighValueLevelThreshold(defaults.getHighValueLevelThreshold());
        }
        if (config.getHighValueExpireMultiplier() == null) {
            config.setHighValueExpireMultiplier(defaults.getHighValueExpireMultiplier());
        }
        if (config.getProtectActiveBusiness() == null) {
            config.setProtectActiveBusiness(defaults.isProtectActiveBusiness());
        }
        if (config.getProtectActiveContract() == null) {
            config.setProtectActiveContract(defaults.isProtectActiveContract());
        }
        if (config.getAutoPoolBatchSize() == null) {
            config.setAutoPoolBatchSize(defaults.getAutoPoolBatchSize());
        } else if (config.getAutoPoolBatchSize() > defaults.getAutoPoolMaxBatchSize()) {
            // Historical tenant overrides must not bypass a newly tightened deployment safety limit.
            config.setAutoPoolBatchSize(defaults.getAutoPoolMaxBatchSize());
        }
        return config;
    }

    @Override
    public int getAutoPoolMaxBatchSize() {
        return poolPolicyProperties.getCustomer().getAutoPoolMaxBatchSize();
    }

    @Override
    @LogRecord(type = CRM_CUSTOMER_POOL_CONFIG_TYPE, subType = CRM_CUSTOMER_POOL_CONFIG_SUB_TYPE, bizNo = "{{#poolConfigId}}",
            success = CRM_CUSTOMER_POOL_CONFIG_SUCCESS)
    public void saveCustomerPoolConfig(CrmCustomerPoolConfigSaveReqVO saveReqVO) {
        int maxBatchSize = getAutoPoolMaxBatchSize();
        if (saveReqVO.getAutoPoolBatchSize() != null && saveReqVO.getAutoPoolBatchSize() > maxBatchSize) {
            throw exception(CUSTOMER_POOL_BATCH_SIZE_EXCEEDS_POLICY, maxBatchSize);
        }
        // 1. 存在，则进行更新
        CrmCustomerPoolConfigDO dbConfig = customerPoolConfigMapper.selectOne();
        CrmCustomerPoolConfigDO poolConfig = BeanUtils.toBean(saveReqVO, CrmCustomerPoolConfigDO.class);
        if (Objects.nonNull(dbConfig)) {
            customerPoolConfigMapper.updateById(poolConfig.setId(dbConfig.getId()));
            // 记录操作日志上下文
            LogRecordContext.putVariable("isPoolConfigUpdate", Boolean.TRUE);
            LogRecordContext.putVariable("poolConfigId", poolConfig.getId());
            return;
        }

        // 2. 不存在，则进行插入
        customerPoolConfigMapper.insert(poolConfig);
        // 记录操作日志上下文
        LogRecordContext.putVariable("isPoolConfigUpdate", Boolean.FALSE);
        LogRecordContext.putVariable("poolConfigId", poolConfig.getId());
    }

}
