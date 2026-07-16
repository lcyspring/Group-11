package com.meession.etm.module.marketing.service.sms;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.marketing.controller.admin.sms.vo.SmsTemplateBatchSendReqVO;
import com.meession.etm.module.marketing.controller.admin.sms.vo.SmsTemplateMarketingPageReqVO;
import com.meession.etm.module.system.dal.dataobject.sms.SmsTemplateDO;
import com.meession.etm.module.system.enums.sms.SmsTemplateTypeEnum;
import com.meession.etm.module.system.service.sms.SmsSendService;
import com.meession.etm.module.system.service.sms.SmsTemplateService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.marketing.enums.ErrorCodeConstants.BATCH_SEND_MOBILE_LIST_EMPTY;
import static com.meession.etm.module.marketing.enums.ErrorCodeConstants.BATCH_SEND_TEMPLATE_CODE_EMPTY;

/**
 * 营销域短信模板 Service 实现类
 * <p>
 * 复用 module-system 的 SMS 能力，提供营销专属的群发功能
 *
 * @author MITEDTSM
 */
@Service
@Slf4j
public class MarketingSmsTemplateServiceImpl implements MarketingSmsTemplateService {

    @Resource
    private SmsTemplateService smsTemplateService;

    @Resource
    private SmsSendService smsSendService;

    @Override
    public PageResult<SmsTemplateDO> getSmsTemplatePage(SmsTemplateMarketingPageReqVO pageReqVO) {
        // 将营销域的请求转换为 system 域的请求，过滤出短信类型的模板
        // 实际上调用 system 模块的分页接口，增加营销类型的过滤
        com.meession.etm.module.system.controller.admin.sms.vo.template.SmsTemplatePageReqVO systemPageReqVO =
                new com.meession.etm.module.system.controller.admin.sms.vo.template.SmsTemplatePageReqVO();
        systemPageReqVO.setPageNo(pageReqVO.getPageNo());
        systemPageReqVO.setPageSize(pageReqVO.getPageSize());
        systemPageReqVO.setType(SmsTemplateTypeEnum.PROMOTION.getType()); // 营销类型
        systemPageReqVO.setStatus(pageReqVO.getStatus());
        systemPageReqVO.setCode(pageReqVO.getCode());
        systemPageReqVO.setContent(pageReqVO.getContent());
        return smsTemplateService.getSmsTemplatePage(systemPageReqVO);
    }

    @Override
    public List<SmsTemplateDO> getSimpleSmsTemplateList() {
        // 获取所有启用状态的营销类型短信模板
        com.meession.etm.module.system.controller.admin.sms.vo.template.SmsTemplatePageReqVO pageReqVO =
                new com.meession.etm.module.system.controller.admin.sms.vo.template.SmsTemplatePageReqVO();
        pageReqVO.setPageSize(com.meession.etm.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        pageReqVO.setType(SmsTemplateTypeEnum.PROMOTION.getType());
        pageReqVO.setStatus(com.meession.etm.framework.common.enums.CommonStatusEnum.ENABLE.getStatus());
        PageResult<SmsTemplateDO> pageResult = smsTemplateService.getSmsTemplatePage(pageReqVO);
        return pageResult.getList();
    }

    @Override
    public List<Long> batchSendSms(SmsTemplateBatchSendReqVO batchSendReqVO) {
        return batchSendSmsByTemplate(batchSendReqVO.getMobiles(),
                batchSendReqVO.getTemplateCode(), batchSendReqVO.getTemplateParams());
    }

    @Override
    public List<Long> batchSendSmsByTemplate(List<String> mobiles, String templateCode, Map<String, Object> templateParams) {
        if (CollUtil.isEmpty(mobiles)) {
            throw exception(BATCH_SEND_MOBILE_LIST_EMPTY);
        }
        if (templateCode == null || templateCode.isBlank()) {
            throw exception(BATCH_SEND_TEMPLATE_CODE_EMPTY);
        }

        List<Long> logIds = new ArrayList<>();
        for (String mobile : mobiles) {
            try {
                // 复用 module-system 的单条发送能力
                Long logId = smsSendService.sendSingleSms(mobile, null, null, templateCode, templateParams);
                logIds.add(logId);
            } catch (Exception e) {
                log.error("[batchSendSmsByTemplate][发送短信异常，手机号({})模板({})]", mobile, templateCode, e);
            }
        }
        log.info("[batchSendSmsByTemplate][批量发送短信完成，总数({})，成功({})]", mobiles.size(), logIds.size());
        return logIds;
    }

}
