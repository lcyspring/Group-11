package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.marketing.vo.mail.CrmMarketingMailTemplatePageReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.mail.CrmMarketingMailTemplateSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingMailTemplateDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * 营销邮件模板 Service 接口
 *
 * @author mitedtsm
 */
public interface CrmMarketingMailTemplateService {

    /**
     * 创建营销邮件模板
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMailTemplate(@Valid CrmMarketingMailTemplateSaveReqVO createReqVO);

    /**
     * 更新营销邮件模板
     *
     * @param updateReqVO 更新信息
     */
    void updateMailTemplate(@Valid CrmMarketingMailTemplateSaveReqVO updateReqVO);

    /**
     * 删除营销邮件模板
     *
     * @param id 编号
     */
    void deleteMailTemplate(Long id);

    /**
     * 获得营销邮件模板
     *
     * @param id 编号
     * @return 营销邮件模板
     */
    CrmMarketingMailTemplateDO getMailTemplate(Long id);

    /**
     * 获得营销邮件模板列表
     *
     * @param ids 编号
     * @return 营销邮件模板列表
     */
    List<CrmMarketingMailTemplateDO> getMailTemplateList(Collection<Long> ids);

    /**
     * 获得营销邮件模板分页
     *
     * @param pageReqVO 分页查询
     * @return 营销邮件模板分页
     */
    PageResult<CrmMarketingMailTemplateDO> getMailTemplatePage(CrmMarketingMailTemplatePageReqVO pageReqVO);

}
