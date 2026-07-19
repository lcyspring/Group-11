package com.meession.etm.module.crm.service.receivable;

import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.receivable.vo.writeoff.CrmReceivableWriteOffCreateReqVO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableWriteOffDO;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableWriteOffMapper;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
public class CrmReceivableWriteOffServiceImpl implements CrmReceivableWriteOffService {
    @Resource private CrmReceivableMapper receivableMapper;
    @Resource private CrmReceivableWriteOffMapper mapper;

    @Override @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE, bizId = "#reqVO.receivableId", level = CrmPermissionLevelEnum.WRITE)
    public Long create(CrmReceivableWriteOffCreateReqVO reqVO) {
        CrmReceivableDO receivable = receivableMapper.selectByIdForUpdate(reqVO.getReceivableId());
        if (receivable == null) throw exception(RECEIVABLE_NOT_EXISTS);
        if (!CrmAuditStatusEnum.APPROVE.getStatus().equals(receivable.getAuditStatus())) throw exception(RECEIVABLE_WRITE_OFF_REQUIRES_APPROVED);
        BigDecimal used = mapper.selectActiveAmount(receivable.getId());
        if (used.add(reqVO.getAmount()).compareTo(receivable.getPrice()) > 0) throw exception(RECEIVABLE_WRITE_OFF_AMOUNT_EXCEEDS_REMAINING);
        String referenceNo = normalizeReferenceNo(reqVO.getReferenceNo());
        if (mapper.selectByReferenceNo(referenceNo) != null) throw exception(RECEIVABLE_WRITE_OFF_REFERENCE_EXISTS);
        CrmReceivableWriteOffDO record = BeanUtils.toBean(reqVO, CrmReceivableWriteOffDO.class)
                .setReferenceNo(referenceNo).setStatus(0);
        try {
            mapper.insert(record);
        } catch (DuplicateKeyException ex) {
            throw exception(RECEIVABLE_WRITE_OFF_REFERENCE_EXISTS);
        }
        return record.getId();
    }

    @Override @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE, bizId = "#receivableId", level = CrmPermissionLevelEnum.WRITE)
    public void reverse(Long receivableId, Long id) {
        if (receivableMapper.selectByIdForUpdate(receivableId) == null) throw exception(RECEIVABLE_NOT_EXISTS);
        CrmReceivableWriteOffDO record = mapper.selectByIdForUpdate(id);
        if (record == null) throw exception(RECEIVABLE_WRITE_OFF_NOT_EXISTS);
        if (!record.getReceivableId().equals(receivableId)) throw exception(RECEIVABLE_WRITE_OFF_NOT_EXISTS);
        if (!Integer.valueOf(0).equals(record.getStatus())) throw exception(RECEIVABLE_WRITE_OFF_ALREADY_REVERSED);
        mapper.updateById(new CrmReceivableWriteOffDO().setId(id).setStatus(10).setReversedAt(LocalDateTime.now()));
    }
    @Override @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE, bizId = "#receivableId", level = CrmPermissionLevelEnum.READ)
    public List<CrmReceivableWriteOffDO> getList(Long receivableId) { return mapper.selectByReceivableId(receivableId); }
    @Override public BigDecimal getWrittenOffAmount(Long receivableId) { return mapper.selectActiveAmount(receivableId); }
    @Override
    public Map<Long, BigDecimal> getWrittenOffAmountMap(Collection<Long> receivableIds) {
        if (receivableIds == null || receivableIds.isEmpty()) return Collections.emptyMap();
        return mapper.selectActiveAmounts(receivableIds).stream().collect(Collectors.toMap(
                item -> item.getReceivableId(), item -> item.getAmount(), BigDecimal::add));
    }

    private static String normalizeReferenceNo(String referenceNo) {
        if (referenceNo == null) return null;
        String normalized = referenceNo.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
