package com.meession.etm.module.crm.service.customer.rule;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.customer.bo.CrmCustomerPoolRecycleRuleConfig;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolRuleTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class PoolRecycleRule extends AbstractPoolRule {

    @Resource
    private CrmCustomerMapper customerMapper;

    @Resource
    private CrmCustomerService customerService;

    @Override
    public Integer getRuleType() {
        return CrmCustomerPoolRuleTypeEnum.RECYCLE.getType();
    }

    @Override
    public void execute(CrmCustomerPoolRuleDO rule) {
        log.info("开始执行公海回收规则: {}", rule.getName());
        CrmCustomerPoolRecycleRuleConfig config = parseConfig(rule, CrmCustomerPoolRecycleRuleConfig.class);

        List<CrmCustomerDO> customers = customerMapper.selectListByAutoPoolConfig(config);

        int count = 0;
        for (CrmCustomerDO customer : customers) {
            try {
                customerService.putCustomerPool(customer.getId());
                count++;
            } catch (Throwable e) {
                log.error("[PoolRecycleRule][客户({}) 放入公海异常]", customer.getId(), e);
            }
        }
        log.info("公海回收规则执行完成: {}, 回收客户数: {}", rule.getName(), count);
    }

}
