package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingCampaignDO;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingCampaignMapper;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingCampaignRelationMapper;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingCampaignStatusEnum;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.MARKETING_CAMPAIGN_STATUS_INVALID;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.MARKETING_PERMISSION_DENIED;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmMarketingServiceImplTest {

    @Mock private CrmMarketingCampaignMapper campaignMapper;
    @Mock private CrmMarketingCampaignRelationMapper relationMapper;
    @Mock private CrmAuthorizationService authorizationService;
    @InjectMocks private CrmMarketingServiceImpl service;

    @Test
    void ownerDeletesDraftAndItsRelationsAtomically() {
        when(campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L,
                CrmMarketingCampaignStatusEnum.DRAFT.getStatus()));
        when(campaignMapper.deleteDraftById(10L)).thenReturn(1);

        service.deleteCampaign(10L, 7L);

        verify(relationMapper).deleteByCampaignId(10L);
        verify(campaignMapper).deleteDraftById(10L);
    }

    @Test
    void activeCampaignCannotBeDeleted() {
        when(campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L,
                CrmMarketingCampaignStatusEnum.ACTIVE.getStatus()));

        assertServiceException(() -> service.deleteCampaign(10L, 7L), MARKETING_CAMPAIGN_STATUS_INVALID);

        verify(relationMapper, never()).deleteByCampaignId(anyLong());
        verify(campaignMapper, never()).deleteDraftById(anyLong());
    }

    @Test
    void unrelatedUserCannotDeleteDraft() {
        when(campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L,
                CrmMarketingCampaignStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> service.deleteCampaign(10L, 8L), MARKETING_PERMISSION_DENIED);

        verify(relationMapper, never()).deleteByCampaignId(anyLong());
        verify(campaignMapper, never()).deleteDraftById(anyLong());
    }

    @Test
    void concurrentStartPreventsDraftDelete() {
        when(campaignMapper.selectById(10L)).thenReturn(campaign(10L, 7L,
                CrmMarketingCampaignStatusEnum.DRAFT.getStatus()));
        when(campaignMapper.deleteDraftById(10L)).thenReturn(0);

        assertServiceException(() -> service.deleteCampaign(10L, 7L), MARKETING_CAMPAIGN_STATUS_INVALID);

        verify(relationMapper, never()).deleteByCampaignId(anyLong());
    }

    private static CrmMarketingCampaignDO campaign(Long id, Long ownerUserId, Integer status) {
        return new CrmMarketingCampaignDO().setId(id).setOwnerUserId(ownerUserId).setStatus(status);
    }
}
