package com.meession.etm.module.crm.service.marketing;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.sms.CrmMarketingSmsTemplatePageReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.sms.CrmMarketingSmsTemplateSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingSmsTemplateDO;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingSmsTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

/**
 * 营销短信模板 Service 实现类
 *
 * @author mitedtsm
 */
@Service
@Validated
public class CrmMarketingSmsTemplateServiceImpl implements CrmMarketingSmsTemplateService {

    /**
     * 正则表达式，匹配 {} 中的变量
     */
    private static final Pattern PATTERN_PARAMS = Pattern.compile("\\{(\\w+)\\}");

    @Resource
    private CrmMarketingSmsTemplateMapper smsTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSmsTemplate(CrmMarketingSmsTemplateSaveReqVO createReqVO) {
        // 校验编码唯一性
        validateCodeUnique(createReqVO.getCode(), null);
        // 插入
        CrmMarketingSmsTemplateDO template = BeanUtils.toBean(createReqVO, CrmMarketingSmsTemplateDO.class);
        // 自动提取参数
        template.setParams(parseTemplateContentParams(template.getContent()));
        smsTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSmsTemplate(CrmMarketingSmsTemplateSaveReqVO updateReqVO) {
        // 校验存在
        validateSmsTemplateExists(updateReqVO.getId());
        // 校验编码唯一性
        validateCodeUnique(updateReqVO.getCode(), updateReqVO.getId());
        // 更新
        CrmMarketingSmsTemplateDO updateObj = BeanUtils.toBean(updateReqVO, CrmMarketingSmsTemplateDO.class);
        // 自动提取参数
        updateObj.setParams(parseTemplateContentParams(updateObj.getContent()));
        smsTemplateMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSmsTemplate(Long id) {
        // 校验存在
        validateSmsTemplateExists(id);
        smsTemplateMapper.deleteById(id);
    }

    @Override
    public CrmMarketingSmsTemplateDO getSmsTemplate(Long id) {
        return smsTemplateMapper.selectById(id);
    }

    @Override
    public List<CrmMarketingSmsTemplateDO> getSmsTemplateList(Collection<Long> ids) {
        return smsTemplateMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<CrmMarketingSmsTemplateDO> getSmsTemplatePage(CrmMarketingSmsTemplatePageReqVO pageReqVO) {
        return smsTemplateMapper.selectPage(pageReqVO);
    }

    /**
     * 校验模板编码唯一性
     *
     * @param code       编码
     * @param excludeId  排除的编号（更新时使用）
     */
    private void validateCodeUnique(String code, Long excludeId) {
        CrmMarketingSmsTemplateDO template = smsTemplateMapper.selectByCode(code);
        if (template == null) {
            return;
        }
        if (excludeId == null || !template.getId().equals(excludeId)) {
            throw exception(MARKETING_SMS_TEMPLATE_CODE_EXISTS);
        }
    }

    /**
     * 校验短信模板存在
     *
     * @param id 编号
     * @return 模板
     */
    private CrmMarketingSmsTemplateDO validateSmsTemplateExists(Long id) {
        CrmMarketingSmsTemplateDO template = smsTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(MARKETING_SMS_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    /**
     * 解析模板内容中的参数名
     *
     * @param content 模板内容
     * @return 参数名列表
     */
    List<String> parseTemplateContentParams(String content) {
        if (content == null || content.isEmpty()) {
            return CollUtil.newArrayList();
        }
        Matcher matcher = PATTERN_PARAMS.matcher(content);
        return matcher.results()
                .map(m -> m.group(1))
                .distinct()
                .collect(Collectors.toList());
    }

}
