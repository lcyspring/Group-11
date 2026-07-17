package com.meession.etm.module.crm.service.receivable;

import com.meession.etm.module.crm.controller.admin.receivable.vo.writeoff.CrmReceivableWriteOffCreateReqVO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableWriteOffDO;
import java.math.BigDecimal;
import java.util.List;

public interface CrmReceivableWriteOffService {
    Long create(CrmReceivableWriteOffCreateReqVO reqVO);
    void reverse(Long receivableId, Long id);
    List<CrmReceivableWriteOffDO> getList(Long receivableId);
    BigDecimal getWrittenOffAmount(Long receivableId);
}
