package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.*;
import com.meession.etm.module.crm.dal.mysql.activity.CrmTaskMapper;
import com.meession.etm.module.crm.dal.mysql.business.CrmBusinessMapper;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.marketing.*;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingCampaignStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingRelationTypeEnum;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
public class CrmMarketingServiceImpl implements CrmMarketingService {
    @Resource private CrmMarketingCampaignMapper campaignMapper;
    @Resource private CrmMarketingCampaignRelationMapper relationMapper;
    @Resource private CrmCompetitorMapper competitorMapper;
    @Resource private CrmAuthorizationService authorizationService;
    @Resource private CrmCustomerMapper customerMapper;
    @Resource private CrmClueMapper clueMapper;
    @Resource private CrmBusinessMapper businessMapper;
    @Resource private CrmTaskMapper taskMapper;
    @Resource private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCampaign(CrmMarketingCampaignSaveReqVO request, Long userId) {
        validateOwner(request.getOwnerUserId(), userId);
        if (request.getEndTime().isBefore(request.getStartTime())
                || request.getEndTime().equals(request.getStartTime())) {
            throw exception(MARKETING_CAMPAIGN_TIME_INVALID);
        }
        CrmMarketingCampaignDO existing = request.getId() == null ? null : requireCampaign(request.getId());
        if (existing != null && !authorizationService.isCrmAdmin(userId)
                && !userId.equals(existing.getOwnerUserId())) throw exception(MARKETING_PERMISSION_DENIED);
        if (existing != null && !CrmMarketingCampaignStatusEnum.DRAFT.getStatus().equals(existing.getStatus())) {
            throw exception(MARKETING_CAMPAIGN_STATUS_INVALID);
        }
        CrmMarketingCampaignDO sameCode = campaignMapper.selectByCode(request.getCode());
        if (sameCode != null && !sameCode.getId().equals(request.getId())) throw exception(MARKETING_CAMPAIGN_CODE_EXISTS);
        CrmMarketingCampaignDO campaign = BeanUtils.toBean(request, CrmMarketingCampaignDO.class);
        if (request.getId() == null) {
            campaign.setStatus(CrmMarketingCampaignStatusEnum.DRAFT.getStatus());
            campaignMapper.insert(campaign);
        } else {
            campaign.setStatus(existing.getStatus());
            campaignMapper.updateById(campaign);
            relationMapper.deleteByCampaignId(campaign.getId());
        }
        Set<String> unique = new LinkedHashSet<>();
        for (CrmMarketingRelationReqVO relation : request.getRelations()) {
            if (!CrmMarketingRelationTypeEnum.isValid(relation.getBizType())
                    || !unique.add(relation.getBizType() + ":" + relation.getBizId())) {
                throw exception(MARKETING_RELATION_NOT_EXISTS);
            }
            validateRelation(relation.getBizType(), relation.getBizId());
            CrmMarketingCampaignRelationDO row = new CrmMarketingCampaignRelationDO()
                    .setCampaignId(campaign.getId()).setBizType(relation.getBizType()).setBizId(relation.getBizId());
            relationMapper.insert(row);
        }
        return campaign.getId();
    }

    @Override
    public void startCampaign(Long id, Long userId) {
        CrmMarketingCampaignDO campaign = requireCampaign(id);
        checkWrite(campaign, userId);
        if (!CrmMarketingCampaignStatusEnum.DRAFT.getStatus().equals(campaign.getStatus()))
            throw exception(MARKETING_CAMPAIGN_STATUS_INVALID);
        campaign.setStatus(CrmMarketingCampaignStatusEnum.ACTIVE.getStatus());
        campaignMapper.updateById(campaign);
    }

    @Override
    public void lockCampaign(Long id, Long userId) {
        CrmMarketingCampaignDO campaign = requireCampaign(id);
        checkWrite(campaign, userId);
        if (!CrmMarketingCampaignStatusEnum.ACTIVE.getStatus().equals(campaign.getStatus()))
            throw exception(MARKETING_CAMPAIGN_STATUS_INVALID);
        campaign.setStatus(CrmMarketingCampaignStatusEnum.LOCKED.getStatus()).setLockedTime(LocalDateTime.now());
        campaignMapper.updateById(campaign);
    }

    @Override
    public void terminateCampaign(CrmMarketingCampaignActionReqVO request, Long userId) {
        CrmMarketingCampaignDO campaign = requireCampaign(request.getId());
        checkWrite(campaign, userId);
        if (CrmMarketingCampaignStatusEnum.TERMINATED.getStatus().equals(campaign.getStatus())
                || CrmMarketingCampaignStatusEnum.COMPLETED.getStatus().equals(campaign.getStatus()))
            throw exception(MARKETING_CAMPAIGN_STATUS_INVALID);
        campaign.setStatus(CrmMarketingCampaignStatusEnum.TERMINATED.getStatus())
                .setTerminatedTime(LocalDateTime.now()).setSummary(request.getSummary());
        campaignMapper.updateById(campaign);
    }

    @Override
    public void completeCampaign(CrmMarketingCampaignActionReqVO request, Long userId) {
        CrmMarketingCampaignDO campaign = requireCampaign(request.getId());
        checkWrite(campaign, userId);
        if (!CrmMarketingCampaignStatusEnum.ACTIVE.getStatus().equals(campaign.getStatus())
                && !CrmMarketingCampaignStatusEnum.LOCKED.getStatus().equals(campaign.getStatus()))
            throw exception(MARKETING_CAMPAIGN_STATUS_INVALID);
        campaign.setStatus(CrmMarketingCampaignStatusEnum.COMPLETED.getStatus())
                .setCompletedTime(LocalDateTime.now()).setSummary(request.getSummary());
        campaignMapper.updateById(campaign);
    }

    @Override
    public CrmMarketingCampaignDO getCampaign(Long id, Long userId) {
        CrmMarketingCampaignDO campaign = requireCampaign(id);
        checkRead(campaign, userId);
        return campaign;
    }

    @Override
    public PageResult<CrmMarketingCampaignDO> getCampaignPage(CrmMarketingCampaignPageReqVO request, Long userId) {
        var scope = authorizationService.resolveOwnerReadScope(userId);
        return campaignMapper.selectPage(request, scope.all(), scope.ownerUserIds());
    }

    @Override
    public List<CrmMarketingCampaignRelationDO> getCampaignRelations(Long id, Long userId) {
        checkRead(requireCampaign(id), userId);
        return relationMapper.selectByCampaignId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCompetitor(CrmCompetitorSaveReqVO request, Long userId) {
        validateOwner(request.getOwnerUserId(), userId);
        CrmCompetitorDO existing = request.getId() == null ? null : competitorMapper.selectById(request.getId());
        if (request.getId() != null && existing == null) throw exception(COMPETITOR_NOT_EXISTS);
        if (existing != null && !authorizationService.isCrmAdmin(userId)
                && !userId.equals(existing.getOwnerUserId())) throw exception(MARKETING_PERMISSION_DENIED);
        CrmCompetitorDO row = BeanUtils.toBean(request, CrmCompetitorDO.class);
        if (row.getStatus() == null) row.setStatus(0);
        if (request.getId() == null) competitorMapper.insert(row); else competitorMapper.updateById(row);
        return row.getId();
    }

    @Override
    public void deleteCompetitor(Long id, Long userId) {
        CrmCompetitorDO row = competitorMapper.selectById(id);
        if (row == null) throw exception(COMPETITOR_NOT_EXISTS);
        if (!authorizationService.isCrmAdmin(userId) && !userId.equals(row.getOwnerUserId()))
            throw exception(MARKETING_PERMISSION_DENIED);
        competitorMapper.deleteById(id);
    }

    @Override
    public PageResult<CrmCompetitorDO> getCompetitorPage(CrmCompetitorPageReqVO request, Long userId) {
        var scope = authorizationService.resolveOwnerReadScope(userId);
        return competitorMapper.selectPage(request, scope.all(), scope.ownerUserIds());
    }

    private CrmMarketingCampaignDO requireCampaign(Long id) {
        CrmMarketingCampaignDO row = campaignMapper.selectById(id);
        if (row == null) throw exception(MARKETING_CAMPAIGN_NOT_EXISTS);
        return row;
    }

    private void checkRead(CrmMarketingCampaignDO row, Long userId) {
        if (authorizationService.isCrmAdmin(userId)) return;
        if (!authorizationService.resolveOwnerReadScope(userId).allows(row.getOwnerUserId()))
            throw exception(MARKETING_PERMISSION_DENIED);
    }

    private void checkWrite(CrmMarketingCampaignDO row, Long userId) {
        if (!authorizationService.isCrmAdmin(userId) && !userId.equals(row.getOwnerUserId()))
            throw exception(MARKETING_PERMISSION_DENIED);
    }

    private void validateOwner(Long ownerUserId, Long userId) {
        if (!authorizationService.isCrmAdmin(userId) && !userId.equals(ownerUserId))
            throw exception(MARKETING_PERMISSION_DENIED);
        adminUserApi.validateUser(ownerUserId);
    }

    private void validateRelation(Integer type, Long id) {
        Object row = switch (type) {
            case 1 -> clueMapper.selectById(id);
            case 2 -> customerMapper.selectById(id);
            case 3 -> businessMapper.selectById(id);
            case 4 -> taskMapper.selectById(id);
            default -> null;
        };
        if (row == null) throw exception(MARKETING_RELATION_NOT_EXISTS);
    }
}
