package com.meession.etm.module.crm.service.marketing;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.mail.CrmMarketingMailTemplatePageReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.mail.CrmMarketingMailTemplateSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingMailTemplateDO;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingMailTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

/**
 * 营销邮件模板 Service 实现类
 *
 * @author mitedtsm
 */
@Service
@Validated
public class CrmMarketingMailTemplateServiceImpl implements CrmMarketingMailTemplateService {

    /**
     * 正则表达式，匹配 {} 中的变量
     */
    private static final Pattern PATTERN_PARAMS = Pattern.compile("\\{(\\w+)\\}");

    @Resource
    private CrmMarketingMailTemplateMapper mailTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMailTemplate(CrmMarketingMailTemplateSaveReqVO createReqVO) {
        // 校验编码唯一性
        validateCodeUnique(createReqVO.getCode(), null);
        // 插入
        CrmMarketingMailTemplateDO template = BeanUtils.toBean(createReqVO, CrmMarketingMailTemplateDO.class);
        // 自动提取参数（标题 + 内容）
        template.setParams(parseTemplateContentParams(template.getTitle(), template.getContent()));
        mailTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMailTemplate(CrmMarketingMailTemplateSaveReqVO updateReqVO) {
        // 校验存在
        validateMailTemplateExists(updateReqVO.getId());
        // 校验编码唯一性
        validateCodeUnique(updateReqVO.getCode(), updateReqVO.getId());
        // 更新
        CrmMarketingMailTemplateDO updateObj = BeanUtils.toBean(updateReqVO, CrmMarketingMailTemplateDO.class);
        // 自动提取参数（标题 + 内容）
        updateObj.setParams(parseTemplateContentParams(updateObj.getTitle(), updateObj.getContent()));
        mailTemplateMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMailTemplate(Long id) {
        // 校验存在
        validateMailTemplateExists(id);
        mailTemplateMapper.deleteById(id);
    }

    @Override
    public CrmMarketingMailTemplateDO getMailTemplate(Long id) {
        return mailTemplateMapper.selectById(id);
    }

    @Override
    public List<CrmMarketingMailTemplateDO> getMailTemplateList(Collection<Long> ids) {
        return mailTemplateMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<CrmMarketingMailTemplateDO> getMailTemplatePage(CrmMarketingMailTemplatePageReqVO pageReqVO) {
        return mailTemplateMapper.selectPage(pageReqVO);
    }

    /**
     * 校验模板编码唯一性
     *
     * @param code       编码
     * @param excludeId  排除的编号（更新时使用）
     */
    private void validateCodeUnique(String code, Long excludeId) {
        CrmMarketingMailTemplateDO template = mailTemplateMapper.selectByCode(code);
        if (template == null) {
            return;
        }
        if (excludeId == null || !template.getId().equals(excludeId)) {
            throw exception(MARKETING_MAIL_TEMPLATE_CODE_EXISTS);
        }
    }

    /**
     * 校验邮件模板存在
     *
     * @param id 编号
     * @return 模板
     */
    private CrmMarketingMailTemplateDO validateMailTemplateExists(Long id) {
        CrmMarketingMailTemplateDO template = mailTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(MARKETING_MAIL_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    /**
     * 解析模板内容中的参数名（标题和内容合并提取）
     *
     * @param title   邮件标题
     * @param content 邮件内容
     * @return 参数名列表
     */
    List<String> parseTemplateContentParams(String title, String content) {
        String combined = Stream.of(title, content)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
        if (combined.isEmpty()) {
            return CollUtil.newArrayList();
        }
        Matcher matcher = PATTERN_PARAMS.matcher(combined);
        return matcher.results()
                .map(m -> m.group(1))
                .distinct()
                .collect(Collectors.toList());
    }

}
