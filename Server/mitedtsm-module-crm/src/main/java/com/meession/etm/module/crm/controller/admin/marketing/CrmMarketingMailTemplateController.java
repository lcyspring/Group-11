package com.meession.etm.module.crm.controller.admin.marketing;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.excel.core.util.ExcelUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.mail.CrmMarketingMailTemplatePageReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.mail.CrmMarketingMailTemplateRespVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.mail.CrmMarketingMailTemplateSaveReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.mail.CrmMarketingMailTemplateSendReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingMailTemplateDO;
import com.meession.etm.module.crm.service.marketing.CrmMarketingMailTemplateService;
import com.meession.etm.module.system.api.mail.MailSendApi;
import com.meession.etm.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 营销邮件模板")
@RestController
@RequestMapping("/crm/marketing-mail-template")
@Validated
public class CrmMarketingMailTemplateController {

    @Resource
    private CrmMarketingMailTemplateService mailTemplateService;

    @Resource
    private MailSendApi mailSendApi;

    @PostMapping("/create")
    @Operation(summary = "创建营销邮件模板")
    @PreAuthorize("@ss.hasPermission('crm:marketing-mail-template:create')")
    public CommonResult<Long> createMailTemplate(@Valid @RequestBody CrmMarketingMailTemplateSaveReqVO createReqVO) {
        return success(mailTemplateService.createMailTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新营销邮件模板")
    @PreAuthorize("@ss.hasPermission('crm:marketing-mail-template:update')")
    public CommonResult<Boolean> updateMailTemplate(@Valid @RequestBody CrmMarketingMailTemplateSaveReqVO updateReqVO) {
        mailTemplateService.updateMailTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除营销邮件模板")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:marketing-mail-template:delete')")
    public CommonResult<Boolean> deleteMailTemplate(@RequestParam("id") Long id) {
        mailTemplateService.deleteMailTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得营销邮件模板")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:marketing-mail-template:query')")
    public CommonResult<CrmMarketingMailTemplateRespVO> getMailTemplate(@RequestParam("id") Long id) {
        CrmMarketingMailTemplateDO template = mailTemplateService.getMailTemplate(id);
        return success(BeanUtils.toBean(template, CrmMarketingMailTemplateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得营销邮件模板分页")
    @PreAuthorize("@ss.hasPermission('crm:marketing-mail-template:query')")
    public CommonResult<PageResult<CrmMarketingMailTemplateRespVO>> getMailTemplatePage(@Valid CrmMarketingMailTemplatePageReqVO pageVO) {
        PageResult<CrmMarketingMailTemplateDO> pageResult = mailTemplateService.getMailTemplatePage(pageVO);
        return success(BeanUtils.toBean(pageResult, CrmMarketingMailTemplateRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出营销邮件模板 Excel")
    @PreAuthorize("@ss.hasPermission('crm:marketing-mail-template:export')")
    public void exportMailTemplateExcel(@Valid CrmMarketingMailTemplatePageReqVO exportReqVO,
                                         HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CrmMarketingMailTemplateDO> list = mailTemplateService.getMailTemplatePage(exportReqVO).getList();
        ExcelUtils.write(response, "营销邮件模板.xls", "数据", CrmMarketingMailTemplateRespVO.class,
                BeanUtils.toBean(list, CrmMarketingMailTemplateRespVO.class));
    }

    @PostMapping("/send")
    @Operation(summary = "群发营销邮件")
    @PreAuthorize("@ss.hasPermission('crm:marketing-mail-template:send')")
    public CommonResult<Boolean> sendMail(@Valid @RequestBody CrmMarketingMailTemplateSendReqVO sendReqVO) {
        // 遍历收件人列表，逐个发送邮件
        MailSendSingleToUserReqDTO reqDTO = new MailSendSingleToUserReqDTO();
        reqDTO.setToMails(sendReqVO.getToMails());
        reqDTO.setCcMails(sendReqVO.getCcMails());
        reqDTO.setBccMails(sendReqVO.getBccMails());
        reqDTO.setTemplateCode(sendReqVO.getTemplateCode());
        reqDTO.setTemplateParams(sendReqVO.getTemplateParams());
        mailSendApi.sendSingleMailToAdmin(reqDTO);
        return success(true);
    }

}
