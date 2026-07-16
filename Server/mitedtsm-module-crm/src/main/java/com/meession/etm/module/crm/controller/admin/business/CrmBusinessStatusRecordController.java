package com.meession.etm.module.crm.controller.admin.business;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.business.vo.status.CrmBusinessStatusRecordRespVO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessStatusDO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessStatusRecordDO;
import com.meession.etm.module.crm.enums.business.CrmBusinessEndStatusEnum;
import com.meession.etm.module.crm.service.business.CrmBusinessStatusRecordService;
import com.meession.etm.module.crm.service.business.CrmBusinessStatusService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - CRM 商机状态流转记录")
@RestController
@RequestMapping("/crm/business-status-record")
public class CrmBusinessStatusRecordController {

    @Resource
    private CrmBusinessStatusRecordService statusRecordService;
    @Resource
    private CrmBusinessStatusService businessStatusService;
    @Resource
    private AdminUserApi adminUserApi;

    @GetMapping("/list")
    @Operation(summary = "获得商机状态流转记录列表")
    @Parameter(name = "businessId", description = "商机编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('crm:business:query')")
    public CommonResult<List<CrmBusinessStatusRecordRespVO>> getStatusRecordList(@RequestParam("businessId") Long businessId) {
        List<CrmBusinessStatusRecordDO> records = statusRecordService.getRecordListByBusinessId(businessId);
        if (records.isEmpty()) {
            return CommonResult.success(BeanUtils.toBean(records, CrmBusinessStatusRecordRespVO.class));
        }

        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSet(records, CrmBusinessStatusRecordDO::getOperatorId));

        Set<Long> statusIds = new HashSet<>();
        for (CrmBusinessStatusRecordDO record : records) {
            if (record.getOldStatusId() != null) {
                statusIds.add(record.getOldStatusId());
            }
            if (record.getNewStatusId() != null) {
                statusIds.add(record.getNewStatusId());
            }
        }

        Map<Long, CrmBusinessStatusDO> statusMap = convertMap(businessStatusService.getBusinessStatusList(statusIds), CrmBusinessStatusDO::getId);

        return CommonResult.success(BeanUtils.toBean(records, CrmBusinessStatusRecordRespVO.class, record -> {
            AdminUserRespDTO user = userMap.get(record.getOperatorId());
            record.setOperatorName(user != null ? user.getNickname() : null);
            record.setOldStatusName(statusMap.get(record.getOldStatusId()) != null ? statusMap.get(record.getOldStatusId()).getName() : null);
            record.setNewStatusName(statusMap.get(record.getNewStatusId()) != null ? statusMap.get(record.getNewStatusId()).getName() : null);
            if (record.getOldEndStatus() != null) {
                record.setOldEndStatusName(CrmBusinessEndStatusEnum.fromStatus(record.getOldEndStatus()).getName());
            }
            if (record.getNewEndStatus() != null) {
                record.setNewEndStatusName(CrmBusinessEndStatusEnum.fromStatus(record.getNewEndStatus()).getName());
            }
        }));
    }

}