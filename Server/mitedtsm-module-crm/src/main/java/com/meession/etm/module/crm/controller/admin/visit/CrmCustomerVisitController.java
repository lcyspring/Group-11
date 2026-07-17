package com.meession.etm.module.crm.controller.admin.visit;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.visit.vo.*;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.visit.CrmCustomerVisitDO;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.visit.CrmCustomerVisitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 客户拜访")
@RestController
@RequestMapping("/crm/customer-visit")
public class CrmCustomerVisitController {
    @Resource private CrmCustomerVisitService visitService;
    @Resource private CrmCustomerService customerService;
    @Resource private CrmContactService contactService;

    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('crm:customer-visit:create')")
    public CommonResult<Long> create(@Valid @RequestBody CrmCustomerVisitCreateReqVO request) {
        return success(visitService.createVisit(getLoginUserId(), request));
    }

    @GetMapping("/get") @PreAuthorize("@ss.hasPermission('crm:customer-visit:query')")
    public CommonResult<CrmCustomerVisitRespVO> get(@RequestParam Long id) {
        return success(enrich(List.of(visitService.getVisit(getLoginUserId(), id))).get(0));
    }

    @GetMapping("/page") @PreAuthorize("@ss.hasPermission('crm:customer-visit:query')")
    public CommonResult<PageResult<CrmCustomerVisitRespVO>> page(@Valid CrmCustomerVisitPageReqVO request) {
        PageResult<CrmCustomerVisitDO> page = visitService.getVisitPage(getLoginUserId(), request);
        return success(new PageResult<>(enrich(page.getList()), page.getTotal()));
    }

    @PostMapping("/result") @PreAuthorize("@ss.hasPermission('crm:customer-visit:update')")
    public CommonResult<Long> result(@Valid @RequestBody CrmCustomerVisitResultReqVO request) {
        return success(visitService.recordResult(getLoginUserId(), request));
    }

    private List<CrmCustomerVisitRespVO> enrich(List<CrmCustomerVisitDO> visits) {
        Map<Long, CrmCustomerDO> customers = customerService.getCustomerMap(
                visits.stream().map(CrmCustomerVisitDO::getCustomerId).filter(Objects::nonNull).distinct().toList());
        Map<Long, CrmContactDO> contacts = contactService.getContactMap(
                visits.stream().map(CrmCustomerVisitDO::getContactId).filter(Objects::nonNull).distinct().toList());
        return visits.stream().map(visit -> {
            CrmCustomerVisitRespVO response = BeanUtils.toBean(visit, CrmCustomerVisitRespVO.class);
            CrmCustomerDO customer = customers.get(visit.getCustomerId());
            CrmContactDO contact = contacts.get(visit.getContactId());
            return response.setCustomerName(customer == null ? null : customer.getName())
                    .setContactName(contact == null ? null : contact.getName());
        }).toList();
    }
}
