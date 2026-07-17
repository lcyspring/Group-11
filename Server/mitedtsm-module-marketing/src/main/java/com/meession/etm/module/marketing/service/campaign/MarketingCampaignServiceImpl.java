package com.meession.etm.module.marketing.service.campaign;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.enums.UserTypeEnum;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.marketing.controller.admin.campaign.vo.CampaignPageReqVO;
import com.meession.etm.module.marketing.controller.admin.campaign.vo.CampaignSaveReqVO;
import com.meession.etm.module.marketing.dal.dataobject.campaign.MarketingCampaignDO;
import com.meession.etm.module.marketing.dal.dataobject.log.MarketingSendRecordDO;
import com.meession.etm.module.marketing.dal.mysql.campaign.MarketingCampaignMapper;
import com.meession.etm.module.marketing.dal.mysql.log.MarketingSendRecordMapper;
import com.meession.etm.module.marketing.enums.CampaignStatusEnum;
import com.meession.etm.module.marketing.enums.CampaignTargetTypeEnum;
import com.meession.etm.module.marketing.enums.CampaignTypeEnum;
import com.meession.etm.module.marketing.service.mail.MarketingMailTemplateService;
import com.meession.etm.module.marketing.service.sms.MarketingSmsTemplateService;
import com.meession.etm.module.member.api.user.MemberUserApi;
import com.meession.etm.module.member.api.user.dto.MemberUserRespDTO;
import com.meession.etm.module.system.service.mail.MailTemplateService;
import com.meession.etm.module.system.service.sms.SmsTemplateService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.marketing.enums.ErrorCodeConstants.*;

/**
 * 营销活动 Service 实现类
 *
 * @author MITEDTSM
 */
@Service
@Slf4j
public class MarketingCampaignServiceImpl implements MarketingCampaignService {

    @Resource
    private MarketingCampaignMapper campaignMapper;

    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @Resource
    private SmsTemplateService smsTemplateService;

    @Resource
    private MailTemplateService mailTemplateService;

    @Resource
    private MarketingSmsTemplateService marketingSmsTemplateService;

    @Resource
    private MarketingMailTemplateService marketingMailTemplateService;

    @Resource
    private MemberUserApi memberUserApi;

    @Resource
    private MarketingSendRecordMapper sendRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCampaign(CampaignSaveReqVO createReqVO) {
        // 校验活动名称是否重复
        validateCampaignNameDuplicate(null, createReqVO.getName());
        // 插入
        MarketingCampaignDO campaign = BeanUtils.toBean(createReqVO, MarketingCampaignDO.class);
        campaign.setStatus(CampaignStatusEnum.DRAFT.getStatus());
        campaign.setSentCount(0);
        campaign.setSuccessCount(0);
        campaign.setFailCount(0);
        campaignMapper.insert(campaign);
        return campaign.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCampaign(CampaignSaveReqVO updateReqVO) {
        // 校验存在
        validateCampaignExists(updateReqVO.getId());
        // 校验活动名称是否重复
        validateCampaignNameDuplicate(updateReqVO.getId(), updateReqVO.getName());
        // 只有草稿状态才能编辑
        MarketingCampaignDO existCampaign = campaignMapper.selectById(updateReqVO.getId());
        if (!CampaignStatusEnum.isDraft(existCampaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_DRAFT);
        }
        // 更新
        MarketingCampaignDO updateObj = BeanUtils.toBean(updateReqVO, MarketingCampaignDO.class);
        campaignMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCampaign(Long id) {
        // 校验存在
        validateCampaignExists(id);
        // 只有草稿状态才能删除
        MarketingCampaignDO existCampaign = campaignMapper.selectById(id);
        if (!CampaignStatusEnum.isDraft(existCampaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_DRAFT);
        }
        // 删除
        campaignMapper.deleteById(id);
    }

    @Override
    public MarketingCampaignDO getCampaign(Long id) {
        return campaignMapper.selectById(id);
    }

    @Override
    public PageResult<MarketingCampaignDO> getCampaignPage(CampaignPageReqVO pageReqVO) {
        return campaignMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitApproval(Long id, Long userId) {
        // 校验存在
        validateCampaignExists(id);
        MarketingCampaignDO campaign = campaignMapper.selectById(id);
        // 只有草稿状态才能提交审核
        if (!CampaignStatusEnum.isDraft(campaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_DRAFT);
        }
        // 创建 BPM 流程实例
        BpmProcessInstanceCreateReqDTO reqDTO = new BpmProcessInstanceCreateReqDTO();
        reqDTO.setProcessDefinitionKey("marketing-campaign-approval");
        reqDTO.setBusinessKey(String.valueOf(campaign.getId()));
        reqDTO.setVariables(Map.of(
                "campaignName", campaign.getName(),
                "campaignType", campaign.getType()
        ));
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId, reqDTO);
        // 更新活动状态和BPM关联
        campaign.setStatus(CampaignStatusEnum.PENDING_APPROVAL.getStatus());
        campaign.setBpmProcessInstanceId(processInstanceId);
        campaignMapper.updateById(campaign);
        log.info("[submitApproval][营销活动({})提交审核，流程实例ID({})]", id, processInstanceId);
        return processInstanceId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startCampaign(Long id) {
        // 校验存在
        validateCampaignExists(id);
        MarketingCampaignDO campaign = campaignMapper.selectById(id);
        // 只有已审核状态才能启动
        if (!CampaignStatusEnum.isApproved(campaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_APPROVED);
        }
        // 更新为进行中状态
        campaign.setStatus(CampaignStatusEnum.IN_PROGRESS.getStatus());
        campaign.setSendTime(LocalDateTime.now());
        campaignMapper.updateById(campaign);

        // 异步执行批量发送（避免阻塞接口返回）
        executeBatchSend(campaign);
    }

    /**
     * 执行批量发送：解析目标用户 → 发送 → 记录关联日志
     */
    private void executeBatchSend(MarketingCampaignDO campaign) {
        try {
            // 1. 解析 templateId → templateCode
            String templateCode = resolveTemplateCode(campaign);
            if (StrUtil.isBlank(templateCode)) {
                log.error("[executeBatchSend][营销活动({})模板ID({})无法解析到模板编码]",
                        campaign.getId(), campaign.getTemplateId());
                return;
            }
            // 2. 获取目标用户列表
            List<MemberUserRespDTO> targetUsers = resolveTargetUsers(campaign);
            if (CollUtil.isEmpty(targetUsers)) {
                log.warn("[executeBatchSend][营销活动({})没有找到目标用户]", campaign.getId());
                return;
            }
            // 3. 根据活动类型分发发送
            List<Long> sendLogIds;
            if (CampaignTypeEnum.isSms(campaign.getType())) {
                List<String> mobiles = targetUsers.stream()
                        .map(MemberUserRespDTO::getMobile)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toList());
                sendLogIds = marketingSmsTemplateService.batchSendSmsByTemplate(
                        mobiles, templateCode, Collections.emptyMap());
            } else {
                // 邮件类型：注意 MemberUserRespDTO 不含 email 字段，此处需后续扩展
                List<String> emails = targetUsers.stream()
                        .map(u -> "")
                        .collect(Collectors.toList());
                sendLogIds = marketingMailTemplateService.batchSendMailByTemplate(
                        emails, templateCode, Collections.emptyMap());
            }
            // 4. 写入关联记录
            String channel = CampaignTypeEnum.isSms(campaign.getType()) ? "SMS" : "MAIL";
            List<MarketingSendRecordDO> records = new ArrayList<>();
            for (Long logId : sendLogIds) {
                MarketingSendRecordDO record = new MarketingSendRecordDO();
                record.setCampaignId(campaign.getId());
                record.setChannel(channel);
                record.setSystemLogId(logId);
                records.add(record);
            }
            sendRecordMapper.insertBatch(records);
            // 5. 更新统计
            updateSendStatistics(campaign.getId(), sendLogIds.size(), sendLogIds.size(), 0);
        } catch (Exception e) {
            log.error("[executeBatchSend][营销活动({})批量发送异常]", campaign.getId(), e);
        }
    }

    /**
     * 解析模板编码：根据活动类型从对应模板服务获取 templateCode
     */
    private String resolveTemplateCode(MarketingCampaignDO campaign) {
        Long templateId = campaign.getTemplateId();
        if (templateId == null) {
            return null;
        }
        if (CampaignTypeEnum.isSms(campaign.getType())) {
            var template = smsTemplateService.getSmsTemplate(templateId);
            return template != null ? template.getCode() : null;
        } else {
            var template = mailTemplateService.getMailTemplate(templateId);
            return template != null ? template.getCode() : null;
        }
    }

    /**
     * 根据 targetType 解析目标用户列表
     */
    private List<MemberUserRespDTO> resolveTargetUsers(MarketingCampaignDO campaign) {
        Integer targetType = campaign.getTargetType();
        if (CampaignTargetTypeEnum.isAllMembers(targetType)) {
            // 全部会员 - 分页获取，防止内存溢出
            // TODO: 实际应该分批获取，目前先取前5000条
            return memberUserApi.getUserListByNickname("");
        }
        if (CampaignTargetTypeEnum.isSpecificUsers(targetType)) {
            // 指定会员 - 解析 targetUserIds JSON
            String userIdsJson = campaign.getTargetUserIds();
            if (StrUtil.isBlank(userIdsJson)) {
                return Collections.emptyList();
            }
            List<Long> userIds = cn.hutool.json.JSONUtil.toList(userIdsJson, Long.class);
            return memberUserApi.getUserList(userIds);
        }
        // 按标签筛选 - TODO: 标签查询接口待对接
        log.warn("[resolveTargetUsers][按标签筛选暂未实现]");
        return Collections.emptyList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelCampaign(Long id) {
        // 校验存在
        validateCampaignExists(id);
        MarketingCampaignDO campaign = campaignMapper.selectById(id);
        // 草稿和已审核状态可以取消
        if (!CampaignStatusEnum.isDraft(campaign.getStatus())
                && !CampaignStatusEnum.isApproved(campaign.getStatus())
                && !CampaignStatusEnum.isPendingApproval(campaign.getStatus())) {
            throw exception(CAMPAIGN_STATUS_NOT_DRAFT);
        }
        // 更新为已终止状态
        campaign.setStatus(CampaignStatusEnum.CANCELLED.getStatus());
        campaign.setEndTime(LocalDateTime.now());
        campaignMapper.updateById(campaign);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSendStatistics(Long id, Integer sentCount, Integer successCount, Integer failCount) {
        MarketingCampaignDO campaign = campaignMapper.selectById(id);
        if (campaign == null) {
            return;
        }
        campaign.setSentCount((campaign.getSentCount() == null ? 0 : campaign.getSentCount()) + sentCount);
        campaign.setSuccessCount((campaign.getSuccessCount() == null ? 0 : campaign.getSuccessCount()) + successCount);
        campaign.setFailCount((campaign.getFailCount() == null ? 0 : campaign.getFailCount()) + failCount);
        campaignMapper.updateById(campaign);
    }

    /**
     * 校验营销活动是否存在
     */
    private void validateCampaignExists(Long id) {
        if (campaignMapper.selectById(id) == null) {
            throw exception(CAMPAIGN_NOT_EXISTS);
        }
    }

    /**
     * 校验营销活动名称是否重复
     */
    private void validateCampaignNameDuplicate(Long id, String name) {
        if (StrUtil.isBlank(name)) {
            return;
        }
        MarketingCampaignDO campaign = campaignMapper.selectByName(name);
        if (campaign == null) {
            return;
        }
        if (id == null || !campaign.getId().equals(id)) {
            throw exception(CAMPAIGN_NAME_DUPLICATE, name);
        }
    }

}
