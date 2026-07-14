package com.meession.etm.module.crm.controller.admin.marketing;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.excel.core.util.ExcelUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.sms.CrmMarketingSmsTemplatePageReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.sms.CrmMarketingSmsTemplateRespVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.sms.CrmMarketingSmsTemplateSaveReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.sms.CrmMarketingSmsTemplateSendReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingSmsTemplateDO;
import com.meession.etm.module.crm.service.marketing.CrmMarketingSmsTemplateService;
import com.meession.etm.module.system.api.sms.SmsSendApi;
import com.meession.etm.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
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
import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 营销短信模板")
@RestController
@RequestMapping("/crm/marketing-sms-template")
@Validated
public class CrmMarketingSmsTemplateController {

    @Resource
    private CrmMarketingSmsTemplateService smsTemplateService;

    @Resource
    private SmsSendApi smsSendApi;

    @PostMapping("/create")
    @Operation(summary = "创建营销短信模板")
    @PreAuthorize("@ss.hasPermission('crm:marketing-sms-template:create')")
    public CommonResult<Long> createSmsTemplate(@Valid @RequestBody CrmMarketingSmsTemplateSaveReqVO createReqVO) {
        return success(smsTemplateService.createSmsTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新营销短信模板")
    @PreAuthorize("@ss.hasPermission('crm:marketing-sms-template:update')")
    public CommonResult<Boolean> updateSmsTemplate(@Valid @RequestBody CrmMarketingSmsTemplateSaveReqVO updateReqVO) {
        smsTemplateService.updateSmsTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除营销短信模板")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:marketing-sms-template:delete')")
    public CommonResult<Boolean> deleteSmsTemplate(@RequestParam("id") Long id) {
        smsTemplateService.deleteSmsTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得营销短信模板")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:marketing-sms-template:query')")
    public CommonResult<CrmMarketingSmsTemplateRespVO> getSmsTemplate(@RequestParam("id") Long id) {
        CrmMarketingSmsTemplateDO template = smsTemplateService.getSmsTemplate(id);
        return success(BeanUtils.toBean(template, CrmMarketingSmsTemplateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得营销短信模板分页")
    @PreAuthorize("@ss.hasPermission('crm:marketing-sms-template:query')")
    public CommonResult<PageResult<CrmMarketingSmsTemplateRespVO>> getSmsTemplatePage(@Valid CrmMarketingSmsTemplatePageReqVO pageVO) {
        PageResult<CrmMarketingSmsTemplateDO> pageResult = smsTemplateService.getSmsTemplatePage(pageVO);
        return success(BeanUtils.toBean(pageResult, CrmMarketingSmsTemplateRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出营销短信模板 Excel")
    @PreAuthorize("@ss.hasPermission('crm:marketing-sms-template:export')")
    public void exportSmsTemplateExcel(@Valid CrmMarketingSmsTemplatePageReqVO exportReqVO,
                                        HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CrmMarketingSmsTemplateDO> list = smsTemplateService.getSmsTemplatePage(exportReqVO).getList();
        ExcelUtils.write(response, "营销短信模板.xls", "数据", CrmMarketingSmsTemplateRespVO.class,
                BeanUtils.toBean(list, CrmMarketingSmsTemplateRespVO.class));
    }

    @PostMapping("/send")
    @Operation(summary = "群发营销短信")
    @PreAuthorize("@ss.hasPermission('crm:marketing-sms-template:send')")
    public CommonResult<Boolean> sendSms(@Valid @RequestBody CrmMarketingSmsTemplateSendReqVO sendReqVO) {
        // 遍历手机号列表，逐个发送短信
        for (String mobile : sendReqVO.getMobiles()) {
            SmsSendSingleToUserReqDTO reqDTO = new SmsSendSingleToUserReqDTO();
            reqDTO.setMobile(mobile);
            reqDTO.setTemplateCode(sendReqVO.getTemplateCode());
            reqDTO.setTemplateParams(sendReqVO.getTemplateParams());
            smsSendApi.sendSingleSmsToAdmin(reqDTO);
        }
        return success(true);
    }

}
