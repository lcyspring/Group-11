package com.meession.etm.module.crm.dal.mysql.marketing;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingConsentDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrmMarketingConsentMapper extends BaseMapperX<CrmMarketingConsentDO> {
    default CrmMarketingConsentDO selectTarget(Long customerId, Long contactId, Integer channel) {
        LambdaQueryWrapperX<CrmMarketingConsentDO> query = new LambdaQueryWrapperX<CrmMarketingConsentDO>()
                .eq(CrmMarketingConsentDO::getCustomerId, customerId)
                .eq(CrmMarketingConsentDO::getChannel, channel);
        if (contactId == null) query.isNull(CrmMarketingConsentDO::getContactId);
        else query.eq(CrmMarketingConsentDO::getContactId, contactId);
        return selectOne(query);
    }
}
