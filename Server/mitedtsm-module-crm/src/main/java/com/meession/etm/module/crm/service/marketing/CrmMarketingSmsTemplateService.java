package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.marketing.vo.sms.CrmMarketingSmsTemplatePageReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.sms.CrmMarketingSmsTemplateSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingSmsTemplateDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * 营销短信模板 Service 接口
 *
 * @author mitedtsm
 */
public interface CrmMarketingSmsTemplateService {

    /**
     * 创建营销短信模板
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSmsTemplate(@Valid CrmMarketingSmsTemplateSaveReqVO createReqVO);

    /**
     * 更新营销短信模板
     *
     * @param updateReqVO 更新信息
     */
    void updateSmsTemplate(@Valid CrmMarketingSmsTemplateSaveReqVO updateReqVO);

    /**
     * 删除营销短信模板
     *
     * @param id 编号
     */
    void deleteSmsTemplate(Long id);

    /**
     * 获得营销短信模板
     *
     * @param id 编号
     * @return 营销短信模板
     */
    CrmMarketingSmsTemplateDO getSmsTemplate(Long id);

    /**
     * 获得营销短信模板列表
     *
     * @param ids 编号
     * @return 营销短信模板列表
     */
    List<CrmMarketingSmsTemplateDO> getSmsTemplateList(Collection<Long> ids);

    /**
     * 获得营销短信模板分页
     *
     * @param pageReqVO 分页查询
     * @return 营销短信模板分页
     */
    PageResult<CrmMarketingSmsTemplateDO> getSmsTemplatePage(CrmMarketingSmsTemplatePageReqVO pageReqVO);

}
