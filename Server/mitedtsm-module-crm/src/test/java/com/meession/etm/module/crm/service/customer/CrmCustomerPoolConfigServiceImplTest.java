package com.meession.etm.module.crm.service.customer;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.customer.vo.poolconfig.CrmCustomerPoolConfigSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolConfigDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerPoolConfigMapper;
import com.meession.etm.module.crm.framework.pool.CrmPoolPolicyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_POOL_BATCH_SIZE_EXCEEDS_POLICY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmCustomerPoolConfigServiceImplTest {

    @Mock
    private CrmCustomerPoolConfigMapper customerPoolConfigMapper;
    @Mock
    private CrmPoolPolicyProperties poolPolicyProperties;
    @InjectMocks
    private CrmCustomerPoolConfigServiceImpl service;

    private CrmPoolPolicyProperties.Customer customerPolicy;

    @BeforeEach
    void setUpPolicy() {
        customerPolicy = new CrmPoolPolicyProperties.Customer();
        customerPolicy.setAutoPoolBatchSize(500);
        customerPolicy.setAutoPoolMaxBatchSize(1000);
        when(poolPolicyProperties.getCustomer()).thenReturn(customerPolicy);
    }

    @Test
    void saveRejectsTenantBatchAboveYamlSafetyLimit() {
        CrmCustomerPoolConfigSaveReqVO request = validRequest().setAutoPoolBatchSize(1001);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.saveCustomerPoolConfig(request));

        assertEquals(CUSTOMER_POOL_BATCH_SIZE_EXCEEDS_POLICY.getCode(), exception.getCode());
        verify(customerPoolConfigMapper, never()).insert(any(CrmCustomerPoolConfigDO.class));
        verify(customerPoolConfigMapper, never()).updateById(any(CrmCustomerPoolConfigDO.class));
    }

    @Test
    void saveAcceptsTenantBatchAtYamlSafetyLimit() {
        CrmCustomerPoolConfigSaveReqVO request = validRequest().setAutoPoolBatchSize(1000);
        when(customerPoolConfigMapper.selectOne()).thenReturn(null);

        service.saveCustomerPoolConfig(request);

        verify(customerPoolConfigMapper).insert(any(CrmCustomerPoolConfigDO.class));
    }

    @Test
    void getConfigClampsHistoricalOverrideToCurrentYamlSafetyLimit() {
        when(customerPoolConfigMapper.selectOne()).thenReturn(
                new CrmCustomerPoolConfigDO().setAutoPoolBatchSize(1200));

        CrmCustomerPoolConfigDO config = service.getCustomerPoolConfig();

        assertEquals(1000, config.getAutoPoolBatchSize());
        assertEquals(1000, service.getAutoPoolMaxBatchSize());
    }

    private static CrmCustomerPoolConfigSaveReqVO validRequest() {
        return new CrmCustomerPoolConfigSaveReqVO()
                .setEnabled(true).setContactExpireDays(30).setDealExpireDays(30)
                .setNotifyEnabled(true).setNotifyDays(2)
                .setDailyClaimLimit(10).setRepeatClaimCooldownDays(30)
                .setHighValueLevelThreshold(4).setHighValueExpireMultiplier(2)
                .setProtectActiveBusiness(true).setProtectActiveContract(true)
                .setAutoPoolBatchSize(500);
    }
}
