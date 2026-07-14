package com.meession.etm.module.crm.dal.mysql.invoice;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceActionRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmInvoiceActionRecordMapper extends BaseMapperX<CrmInvoiceActionRecordDO> {

    default List<CrmInvoiceActionRecordDO> selectListByInvoiceId(Long invoiceId) {
        return selectList(new LambdaQueryWrapperX<CrmInvoiceActionRecordDO>()
                .eq(CrmInvoiceActionRecordDO::getInvoiceId, invoiceId)
                .orderByAsc(CrmInvoiceActionRecordDO::getId));
    }
}
