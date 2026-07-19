package com.meession.etm.module.crm.service.clue;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.clue.vo.publicpool.CrmCluePublicPageReqVO;
import com.meession.etm.module.crm.controller.admin.clue.vo.publicpool.CrmCluePublicPutReqVO;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import jakarta.validation.Valid;

import java.util.List;

public interface CrmCluePublicPoolService {

    PageResult<CrmClueDO> getPublicPage(@Valid CrmCluePublicPageReqVO pageReqVO);

    void putCluePublic(@Valid CrmCluePublicPutReqVO reqVO, Long userId);

    void claimPublicClues(List<Long> clueIds, Long userId);

    void assignPublicClues(List<Long> clueIds, Long ownerUserId, Long operatorUserId);

    int autoPutCluePublicPool();
}
