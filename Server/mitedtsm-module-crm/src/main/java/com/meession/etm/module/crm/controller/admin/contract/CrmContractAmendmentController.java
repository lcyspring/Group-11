package com.meession.etm.module.crm.controller.admin.contract;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.module.crm.controller.admin.contract.vo.amendment.CrmContractAmendmentCommandReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.amendment.CrmContractAmendmentRespVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.amendment.CrmContractAmendmentSaveReqVO;
import com.meession.etm.module.crm.service.contract.CrmContractAmendmentService;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractProductDO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/crm/contract-amendment")
public class CrmContractAmendmentController {

    @Resource
    private CrmContractAmendmentService service;

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('crm:contract:amendment')")
    public CommonResult<Long> create(@Valid @RequestBody CrmContractAmendmentSaveReqVO req) {
        return success(service.createAmendment(req, getLoginUserId()));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('crm:contract:amendment')")
    public CommonResult<Boolean> update(@Valid @RequestBody CrmContractAmendmentSaveReqVO req) {
        service.updateAmendment(req, getLoginUserId());
        return success(true);
    }

    @PutMapping("/submit")
    @PreAuthorize("@ss.hasPermission('crm:contract:amendment')")
    public CommonResult<Boolean> submit(@Valid @RequestBody CrmContractAmendmentCommandReqVO req) {
        service.submitAmendment(req.getContractId(), req.getId(), getLoginUserId());
        return success(true);
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('crm:contract:query')")
    public CommonResult<CrmContractAmendmentRespVO> get(@RequestParam Long contractId, @RequestParam Long id) {
        var amendment = service.getAmendment(contractId, id);
        CrmContractAmendmentRespVO result = BeanUtils.toBean(amendment, CrmContractAmendmentRespVO.class);
        CrmContractDO after = JsonUtils.parseObject(amendment.getAfterContractSnapshot(), CrmContractDO.class);
        result.setContractName(after.getName()).setStartTime(after.getStartTime()).setEndTime(after.getEndTime())
                .setDiscountPercent(after.getDiscountPercent()).setSignContactId(after.getSignContactId())
                .setSignUserId(after.getSignUserId()).setRemark(after.getRemark())
                .setProducts(BeanUtils.toBean(JsonUtils.parseArray(amendment.getAfterProductSnapshot(),
                        CrmContractProductDO.class), CrmContractAmendmentRespVO.Product.class));
        return success(result);
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('crm:contract:query')")
    public CommonResult<List<CrmContractAmendmentRespVO>> list(@RequestParam Long contractId) {
        return success(BeanUtils.toBean(service.getAmendmentList(contractId), CrmContractAmendmentRespVO.class));
    }
}
