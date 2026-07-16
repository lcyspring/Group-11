package com.meession.etm.module.marketing.service.mail;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.common.enums.CommonStatusEnum;
import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.marketing.controller.admin.mail.vo.MailTemplateBatchSendReqVO;
import com.meession.etm.module.marketing.controller.admin.mail.vo.MailTemplateMarketingPageReqVO;
import com.meession.etm.module.system.dal.dataobject.mail.MailTemplateDO;
import com.meession.etm.module.system.service.mail.MailSendService;
import com.meession.etm.module.system.service.mail.MailTemplateService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.marketing.enums.ErrorCodeConstants.BATCH_SEND_MAIL_LIST_EMPTY;
import static com.meession.etm.module.marketing.enums.ErrorCodeConstants.BATCH_SEND_TEMPLATE_CODE_EMPTY;

/**
 * 营销域邮件模板 Service 实现类
 * <p>
 * 复用 module-system 的 Mail 能力，提供营销专属的群发功能
 *
 * @author MITEDTSM
 */
@Service
@Slf4j
public class MarketingMailTemplateServiceImpl implements MarketingMailTemplateService {

    @Resource
    private MailTemplateService mailTemplateService;

    @Resource
    private MailSendService mailSendService;

    @Override
    public PageResult<MailTemplateDO> getMailTemplatePage(MailTemplateMarketingPageReqVO pageReqVO) {
        // 将营销域的请求转换为 system 域的请求
        com.meession.etm.module.system.controller.admin.mail.vo.template.MailTemplatePageReqVO systemPageReqVO =
                new com.meession.etm.module.system.controller.admin.mail.vo.template.MailTemplatePageReqVO();
        systemPageReqVO.setPageNo(pageReqVO.getPageNo());
        systemPageReqVO.setPageSize(pageReqVO.getPageSize());
        systemPageReqVO.setStatus(pageReqVO.getStatus());
        systemPageReqVO.setName(pageReqVO.getName());
        systemPageReqVO.setCode(pageReqVO.getCode());
        return mailTemplateService.getMailTemplatePage(systemPageReqVO);
    }

    @Override
    public List<MailTemplateDO> getSimpleMailTemplateList() {
        // 获取所有启用状态的邮件模板
        com.meession.etm.module.system.controller.admin.mail.vo.template.MailTemplatePageReqVO pageReqVO =
                new com.meession.etm.module.system.controller.admin.mail.vo.template.MailTemplatePageReqVO();
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        pageReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        PageResult<MailTemplateDO> pageResult = mailTemplateService.getMailTemplatePage(pageReqVO);
        return pageResult.getList();
    }

    @Override
    public List<Long> batchSendMail(MailTemplateBatchSendReqVO batchSendReqVO) {
        return batchSendMailByTemplate(batchSendReqVO.getToMails(),
                batchSendReqVO.getTemplateCode(), batchSendReqVO.getTemplateParams());
    }

    @Override
    public List<Long> batchSendMailByTemplate(List<String> toMails, String templateCode, Map<String, Object> templateParams) {
        if (CollUtil.isEmpty(toMails)) {
            throw exception(BATCH_SEND_MAIL_LIST_EMPTY);
        }
        if (templateCode == null || templateCode.isBlank()) {
            throw exception(BATCH_SEND_TEMPLATE_CODE_EMPTY);
        }

        List<Long> logIds = new ArrayList<>();
        for (String toMail : toMails) {
            try {
                // 复用 module-system 的邮件发送能力
                Long logId = mailSendService.sendSingleMail(
                        Collections.singletonList(toMail), null, null,
                        null, null, templateCode, templateParams);
                logIds.add(logId);
            } catch (Exception e) {
                log.error("[batchSendMailByTemplate][发送邮件异常，邮箱({})模板({})]", toMail, templateCode, e);
            }
        }
        log.info("[batchSendMailByTemplate][批量发送邮件完成，总数({})，成功({})]", toMails.size(), logIds.size());
        return logIds;
    }

}
