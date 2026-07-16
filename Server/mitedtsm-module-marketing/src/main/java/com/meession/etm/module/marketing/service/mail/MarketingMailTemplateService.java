package com.meession.etm.module.marketing.service.mail;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.marketing.controller.admin.mail.vo.MailTemplateBatchSendReqVO;
import com.meession.etm.module.marketing.controller.admin.mail.vo.MailTemplateMarketingPageReqVO;
import com.meession.etm.module.system.dal.dataobject.mail.MailTemplateDO;

import java.util.List;
import java.util.Map;

/**
 * 营销域邮件模板 Service 接口
 *
 * @author MITEDTSM
 */
public interface MarketingMailTemplateService {

    /**
     * 获得邮件模板分页（营销视角）
     *
     * @param pageReqVO 分页查询
     * @return 邮件模板分页
     */
    PageResult<MailTemplateDO> getMailTemplatePage(MailTemplateMarketingPageReqVO pageReqVO);

    /**
     * 获得可用邮件模板精简列表（供营销活动选择）
     *
     * @return 邮件模板精简列表
     */
    List<MailTemplateDO> getSimpleMailTemplateList();

    /**
     * 批量发送邮件
     *
     * @param batchSendReqVO 批量发送请求
     * @return 发送日志编号列表
     */
    List<Long> batchSendMail(MailTemplateBatchSendReqVO batchSendReqVO);

    /**
     * 根据模板编码和参数批量发送邮件（供营销活动自动化调用）
     *
     * @param toMails        收件邮箱列表
     * @param templateCode   模板编码
     * @param templateParams 模板参数
     * @return 发送日志编号列表
     */
    List<Long> batchSendMailByTemplate(List<String> toMails, String templateCode, Map<String, Object> templateParams);

}
