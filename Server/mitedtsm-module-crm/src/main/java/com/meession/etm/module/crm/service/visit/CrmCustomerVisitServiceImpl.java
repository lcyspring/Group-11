package com.meession.etm.module.crm.service.visit;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import com.meession.etm.module.crm.controller.admin.followup.vo.CrmFollowUpRecordSaveReqVO;
import com.meession.etm.module.crm.controller.admin.visit.vo.CrmCustomerVisitCreateReqVO;
import com.meession.etm.module.crm.controller.admin.visit.vo.CrmCustomerVisitPageReqVO;
import com.meession.etm.module.crm.controller.admin.visit.vo.CrmCustomerVisitResultReqVO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.visit.CrmCustomerVisitDO;
import com.meession.etm.module.crm.dal.mysql.visit.CrmCustomerVisitMapper;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.followup.CrmFollowUpRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class CrmCustomerVisitServiceImpl implements CrmCustomerVisitService {
    public static final String PROCESS_KEY = "crm_customer_visit_audit";
    private static final int RESULT_PENDING = 0;
    private static final int RESULT_COMPLETED = 1;
    private static final int FOLLOW_UP_TYPE_VISIT = 3;

    @Resource private CrmCustomerVisitMapper visitMapper;
    @Resource private CrmCustomerService customerService;
    @Resource private CrmContactService contactService;
    @Resource private CrmFollowUpRecordService followUpRecordService;
    @Resource private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#request.customerId", level = CrmPermissionLevelEnum.WRITE)
    public Long createVisit(Long userId, CrmCustomerVisitCreateReqVO request) {
        customerService.validateCustomer(request.getCustomerId());
        validateContact(request.getCustomerId(), request.getContactId());
        List<Long> participants = request.getParticipantUserIds() == null ? List.of()
                : request.getParticipantUserIds().stream().filter(Objects::nonNull).distinct().toList();
        CrmCustomerVisitDO visit = BeanUtils.toBean(request, CrmCustomerVisitDO.class)
                .setApplicantUserId(userId).setParticipantUserIds(participants)
                .setAuditStatus(BpmTaskStatusEnum.RUNNING.getStatus()).setResultStatus(RESULT_PENDING);
        visitMapper.insert(visit);
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", visit.getCustomerId());
        variables.put("contactId", visit.getContactId());
        variables.put("plannedStartTime", visit.getPlannedStartTime());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(visit.getId())).setVariables(variables)
                        .setStartUserSelectAssignees(request.getStartUserSelectAssignees()));
        visitMapper.updateById(new CrmCustomerVisitDO().setId(visit.getId()).setProcessInstanceId(processInstanceId));
        return visit.getId();
    }

    private void validateContact(Long customerId, Long contactId) {
        if (contactId == null) return;
        CrmContactDO contact = contactService.getContact(contactId);
        if (contact == null || !Objects.equals(contact.getCustomerId(), customerId)) {
            throw exception(CUSTOMER_VISIT_CONTACT_MISMATCH);
        }
    }

    @Override
    public CrmCustomerVisitDO getVisit(Long userId, Long id) {
        CrmCustomerVisitDO visit = visitMapper.selectById(id);
        if (visit == null || !Objects.equals(visit.getApplicantUserId(), userId)) {
            throw exception(CUSTOMER_VISIT_NOT_EXISTS);
        }
        return visit;
    }

    @Override
    public PageResult<CrmCustomerVisitDO> getVisitPage(Long userId, CrmCustomerVisitPageReqVO request) {
        return visitMapper.selectPage(userId, request);
    }

    @Override
    public void updateAuditStatus(Long id, String processInstanceId, Integer status) {
        CrmCustomerVisitDO visit = visitMapper.selectById(id);
        if (visit == null) throw exception(CUSTOMER_VISIT_NOT_EXISTS);
        CrmCustomerVisitDO update = new CrmCustomerVisitDO().setId(id).setAuditStatus(status)
                .setProcessInstanceId(processInstanceId);
        if (BpmTaskStatusEnum.APPROVE.getStatus().equals(status)
                || BpmTaskStatusEnum.REJECT.getStatus().equals(status)) {
            update.setApprovalTime(LocalDateTime.now());
        }
        visitMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordResult(Long userId, CrmCustomerVisitResultReqVO request) {
        CrmCustomerVisitDO visit = visitMapper.selectByIdForUpdate(request.getId());
        if (visit == null || !Objects.equals(visit.getApplicantUserId(), userId)) {
            throw exception(CUSTOMER_VISIT_NOT_EXISTS);
        }
        if (!BpmTaskStatusEnum.APPROVE.getStatus().equals(visit.getAuditStatus())
                || !Integer.valueOf(RESULT_PENDING).equals(visit.getResultStatus())) {
            throw exception(CUSTOMER_VISIT_RESULT_STATUS_INVALID);
        }
        if (visit.getFollowUpRecordId() != null) throw exception(CUSTOMER_VISIT_RESULT_ALREADY_RECORDED);
        if (request.getActualEndTime().isAfter(LocalDateTime.now())
                || request.getActualEndTime().isBefore(request.getActualStartTime())) {
            throw exception(CUSTOMER_VISIT_RESULT_TIME_INVALID);
        }
        CrmFollowUpRecordSaveReqVO followUp = new CrmFollowUpRecordSaveReqVO()
                .setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType()).setBizId(visit.getCustomerId())
                .setType(FOLLOW_UP_TYPE_VISIT).setContent(request.getResultContent())
                .setNextTime(request.getNextContactTime()).setFileUrls(request.getResultAttachmentUrls())
                .setContactIds(visit.getContactId() == null ? List.of() : List.of(visit.getContactId()));
        Long followUpId = followUpRecordService.createFollowUpRecord(followUp);
        visitMapper.updateById(BeanUtils.toBean(request, CrmCustomerVisitDO.class)
                .setResultStatus(RESULT_COMPLETED).setFollowUpRecordId(followUpId));
        return followUpId;
    }
}
