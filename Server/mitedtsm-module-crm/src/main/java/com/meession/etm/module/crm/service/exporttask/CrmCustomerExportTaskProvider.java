package com.meession.etm.module.crm.service.exporttask;

import com.meession.etm.framework.excel.core.util.ExcelUtils;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerRespVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.service.customer.CrmCustomerResponseAssembler;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.EXPORT_TASK_OBJECT_CHANGED;
import static com.meession.etm.module.crm.enums.exporttask.CrmExportObjectType.CUSTOMER;

@Component
@RequiredArgsConstructor
public class CrmCustomerExportTaskProvider implements CrmExportTaskProvider {
    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CrmCustomerService customerService;
    private final CrmCustomerResponseAssembler responseAssembler;

    @Override public String objectType() { return CUSTOMER; }
    @Override public Integer bizType() { return CrmBizTypeEnum.CRM_CUSTOMER.getType(); }

    @Override
    public void validateObjects(List<Long> objectIds) {
        if (customerService.getCustomerList(objectIds).size() != objectIds.size()) {
            throw exception(EXPORT_TASK_OBJECT_CHANGED);
        }
    }

    @Override
    public ExportFile generate(List<Long> objectIds, Long userId) {
        List<CrmCustomerDO> customers = customerService.getCustomerList(objectIds);
        Map<Long, CrmCustomerDO> byId = new LinkedHashMap<>();
        customers.forEach(customer -> byId.put(customer.getId(), customer));
        if (byId.size() != objectIds.size()) {
            throw exception(EXPORT_TASK_OBJECT_CHANGED);
        }
        List<CrmCustomerDO> ordered = objectIds.stream().map(byId::get).toList();
        List<CrmCustomerRespVO> rows = responseAssembler.buildDetailList(ordered);
        return new ExportFile(ExcelUtils.writeBytes("客户", CrmCustomerRespVO.class, rows),
                "客户导出.xlsx", CONTENT_TYPE);
    }
}
