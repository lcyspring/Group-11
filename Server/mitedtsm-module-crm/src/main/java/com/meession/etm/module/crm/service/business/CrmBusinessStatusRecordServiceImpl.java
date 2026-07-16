package com.meession.etm.module.crm.service.business;

import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessStatusRecordDO;
import com.meession.etm.module.crm.dal.mysql.business.CrmBusinessStatusRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CrmBusinessStatusRecordServiceImpl implements CrmBusinessStatusRecordService {

    @Resource
    private CrmBusinessStatusRecordMapper statusRecordMapper;

    @Override
    public void createRecord(CrmBusinessDO oldBusiness, CrmBusinessDO newBusiness, Long operatorId, String remark) {
        CrmBusinessStatusRecordDO record = CrmBusinessStatusRecordDO.builder()
                .businessId(oldBusiness.getId())
                .oldStatusTypeId(oldBusiness.getStatusTypeId())
                .oldStatusId(oldBusiness.getStatusId())
                .oldEndStatus(oldBusiness.getEndStatus())
                .newStatusTypeId(newBusiness.getStatusTypeId())
                .newStatusId(newBusiness.getStatusId())
                .newEndStatus(newBusiness.getEndStatus())
                .operatorId(operatorId)
                .remark(remark)
                .build();
        record.setCreateTime(LocalDateTime.now());
        statusRecordMapper.insert(record);
    }

    @Override
    public List<CrmBusinessStatusRecordDO> getRecordListByBusinessId(Long businessId) {
        return statusRecordMapper.selectListByBusinessId(businessId);
    }

}