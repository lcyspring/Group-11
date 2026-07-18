package com.meession.etm.module.crm.service.customer;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.common.util.collection.MapUtils;
import com.meession.etm.framework.common.util.number.NumberUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.ip.core.utils.AreaUtils;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerRespVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.dept.dto.DeptRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;

@Component
@RequiredArgsConstructor
public class CrmCustomerResponseAssembler {
    private final CrmCustomerService customerService;
    private final CrmContactService contactService;
    private final AdminUserApi adminUserApi;
    private final DeptApi deptApi;

    public List<CrmCustomerRespVO> buildDetailList(List<CrmCustomerDO> list) {
        if (CollUtil.isEmpty(list)) {
            return java.util.Collections.emptyList();
        }
        Map<Long, CrmContactDO> primaryContactMap = convertMap(
                contactService.getPrimaryContactListByCustomerIds(convertSet(list, CrmCustomerDO::getId)),
                CrmContactDO::getCustomerId);
        Set<Long> parentCustomerIds = convertSet(list, CrmCustomerDO::getParentCustomerId);
        parentCustomerIds.remove(null);
        Map<Long, CrmCustomerDO> parentCustomerMap = customerService.getCustomerMap(parentCustomerIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSetByFlatMap(list,
                customer -> Stream.of(NumberUtils.parseLong(customer.getCreator()), customer.getOwnerUserId(),
                        customer.getPoolPreviousOwnerUserId())));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        Map<Long, Long> poolDayMap = customerService.getPoolDayMap(list);
        return BeanUtils.toBean(list, CrmCustomerRespVO.class, customerVO -> {
            customerVO.setAreaName(AreaUtils.format(customerVO.getAreaId()));
            MapUtils.findAndThen(primaryContactMap, customerVO.getId(), contact -> customerVO
                    .setPrimaryContactName(contact.getName()).setPrimaryContactMobile(contact.getMobile()));
            MapUtils.findAndThen(parentCustomerMap, customerVO.getParentCustomerId(), parent ->
                    customerVO.setParentCustomerName(parent.getName()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(customerVO.getCreator()),
                    user -> customerVO.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, customerVO.getOwnerUserId(), user -> {
                customerVO.setOwnerUserName(user.getNickname());
                MapUtils.findAndThen(deptMap, user.getDeptId(), dept -> customerVO.setOwnerUserDeptName(dept.getName()));
            });
            MapUtils.findAndThen(userMap, customerVO.getPoolPreviousOwnerUserId(),
                    user -> customerVO.setPoolPreviousOwnerUserName(user.getNickname()));
            if (customerVO.getOwnerUserId() != null) {
                customerVO.setPoolDay(poolDayMap.get(customerVO.getId()));
            }
        });
    }
}
