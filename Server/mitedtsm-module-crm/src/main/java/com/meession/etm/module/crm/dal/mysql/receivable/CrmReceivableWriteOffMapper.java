package com.meession.etm.module.crm.dal.mysql.receivable;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableWriteOffDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CrmReceivableWriteOffMapper extends BaseMapperX<CrmReceivableWriteOffDO> {
    default List<CrmReceivableWriteOffDO> selectByReceivableId(Long receivableId) {
        return selectList(new LambdaQueryWrapper<CrmReceivableWriteOffDO>()
                .eq(CrmReceivableWriteOffDO::getReceivableId, receivableId).orderByDesc(CrmReceivableWriteOffDO::getId));
    }
    @Select("SELECT COALESCE(SUM(amount),0) FROM crm_receivable_write_off WHERE receivable_id=#{receivableId} AND status=0 AND deleted=0")
    BigDecimal selectActiveAmount(Long receivableId);
    default CrmReceivableWriteOffDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapper<CrmReceivableWriteOffDO>().eq(CrmReceivableWriteOffDO::getId, id).last("FOR UPDATE"));
    }
    default CrmReceivableWriteOffDO selectByReferenceNo(String referenceNo) {
        return referenceNo == null || referenceNo.isBlank() ? null : selectOne(CrmReceivableWriteOffDO::getReferenceNo, referenceNo);
    }
}
