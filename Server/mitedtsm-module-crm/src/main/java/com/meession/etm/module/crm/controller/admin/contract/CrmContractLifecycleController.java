package com.meession.etm.module.crm.controller.admin.contract;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractAttachmentCreateReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractLifecycleRespVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractSignReqVO;
import com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle.CrmContractSignVoidReqVO;
import com.meession.etm.module.crm.service.contract.CrmContractLifecycleService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/crm/contract-lifecycle")
public class CrmContractLifecycleController {

    @Resource
    private CrmContractLifecycleService service;

    @PostMapping("/attachment")
    @PreAuthorize("@ss.hasPermission('crm:contract:attachment')")
    public CommonResult<Long> createAttachment(@Valid @RequestBody CrmContractAttachmentCreateReqVO req) {
        return success(service.createAttachment(req, getLoginUserId()));
    }

    @DeleteMapping("/attachment")
    @PreAuthorize("@ss.hasPermission('crm:contract:attachment')")
    public CommonResult<Boolean> deleteAttachment(@RequestParam Long contractId, @RequestParam Long attachmentId) {
        service.deleteAttachment(contractId, attachmentId);
        return success(true);
    }

    @PutMapping("/sign")
    @PreAuthorize("@ss.hasPermission('crm:contract:sign')")
    public CommonResult<Long> sign(@Valid @RequestBody CrmContractSignReqVO req) {
        return success(service.sign(req, getLoginUserId()));
    }

    @PutMapping("/sign-void")
    @PreAuthorize("@ss.hasPermission('crm:contract:sign-void')")
    public CommonResult<Boolean> voidSign(@Valid @RequestBody CrmContractSignVoidReqVO req) {
        service.voidSign(req, getLoginUserId());
        return success(true);
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('crm:contract:query')")
    public CommonResult<CrmContractLifecycleRespVO> get(@RequestParam Long contractId) {
        return success(new CrmContractLifecycleRespVO(
                BeanUtils.toBean(service.getSigning(contractId), CrmContractLifecycleRespVO.Signing.class),
                BeanUtils.toBean(service.getAttachments(contractId), CrmContractLifecycleRespVO.Attachment.class),
                BeanUtils.toBean(service.getChangeRecords(contractId), CrmContractLifecycleRespVO.ChangeRecord.class),
                service.getSupportedSignMethods()));
    }
}
