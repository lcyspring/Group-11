package com.meession.etm.module.marketing.service.sms;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.marketing.controller.admin.sms.vo.SmsTemplateBatchSendReqVO;
import com.meession.etm.module.marketing.controller.admin.sms.vo.SmsTemplateMarketingPageReqVO;
import com.meession.etm.module.system.dal.dataobject.sms.SmsTemplateDO;

import java.util.List;
import java.util.Map;

/**
 * 营销域短信模板 Service 接口
 *
 * @author MITEDTSM
 */
public interface MarketingSmsTemplateService {

    /**
     * 获得短信模板分页（营销视角）
     *
     * @param pageReqVO 分页查询
     * @return 短信模板分页
     */
    PageResult<SmsTemplateDO> getSmsTemplatePage(SmsTemplateMarketingPageReqVO pageReqVO);

    /**
     * 获得可用短信模板精简列表（供营销活动选择）
     *
     * @return 短信模板精简列表
     */
    List<SmsTemplateDO> getSimpleSmsTemplateList();

    /**
     * 批量发送短信
     *
     * @param batchSendReqVO 批量发送请求
     * @return 发送日志编号列表
     */
    List<Long> batchSendSms(SmsTemplateBatchSendReqVO batchSendReqVO);

    /**
     * 根据模板编码和参数批量发送短信（供营销活动自动化调用）
     *
     * @param mobiles        手机号列表
     * @param templateCode   模板编码
     * @param templateParams 模板参数
     * @return 发送日志编号列表
     */
    List<Long> batchSendSmsByTemplate(List<String> mobiles, String templateCode, Map<String, Object> templateParams);

}
