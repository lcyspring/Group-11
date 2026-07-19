package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmMarketingRecipientRespVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastRecipientDO;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 群发收件人响应组装器。
 *
 * 历史收件人名称必须按收件人快照中的编号批量解析，不能依赖当前用户可选目标列表；后者会随
 * 数据权限、负责人和客户状态变化，无法稳定展示历史结果。
 */
@Component
public class CrmMarketingRecipientResponseAssembler {

    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmContactService contactService;

    public PageResult<CrmMarketingRecipientRespVO> assemble(
            PageResult<CrmMarketingBroadcastRecipientDO> page) {
        List<CrmMarketingBroadcastRecipientDO> rows = page.getList();
        Set<Long> customerIds = rows.stream().map(CrmMarketingBroadcastRecipientDO::getCustomerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contactIds = rows.stream().map(CrmMarketingBroadcastRecipientDO::getContactId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, CrmCustomerDO> customerMap = customerIds.isEmpty()
                ? Map.of() : customerService.getCustomerMap(customerIds);
        Map<Long, CrmContactDO> contactMap = contactIds.isEmpty()
                ? Map.of() : contactService.getContactMap(contactIds);
        List<CrmMarketingRecipientRespVO> responses = BeanUtils.toBean(rows, CrmMarketingRecipientRespVO.class);
        responses.forEach(response -> {
            CrmCustomerDO customer = response.getCustomerId() == null
                    ? null : customerMap.get(response.getCustomerId());
            if ((response.getCustomerName() == null || response.getCustomerName().isBlank()) && customer != null) {
                response.setCustomerName(customer.getName());
            }
            CrmContactDO contact = response.getContactId() == null
                    ? null : contactMap.get(response.getContactId());
            if ((response.getContactName() == null || response.getContactName().isBlank()) && contact != null) {
                response.setContactName(contact.getName());
            }
        });
        return new PageResult<>(responses, page.getTotal());
    }
}
