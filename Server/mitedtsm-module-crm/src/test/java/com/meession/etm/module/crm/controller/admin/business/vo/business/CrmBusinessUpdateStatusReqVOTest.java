package com.meession.etm.module.crm.controller.admin.business.vo.business;

import com.meession.etm.module.crm.enums.business.CrmBusinessEndStatusEnum;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmBusinessUpdateStatusReqVOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void lostBusinessRequiresEndRemark() {
        CrmBusinessUpdateStatusReqVO reqVO = new CrmBusinessUpdateStatusReqVO()
                .setId(1L).setEndStatus(CrmBusinessEndStatusEnum.LOSE.getStatus());

        assertEquals(1, validator.validate(reqVO).size());
    }

    @Test
    void invalidBusinessAcceptsReason() {
        CrmBusinessUpdateStatusReqVO reqVO = new CrmBusinessUpdateStatusReqVO()
                .setId(1L).setEndStatus(CrmBusinessEndStatusEnum.INVALID.getStatus())
                .setEndRemark("客户资料重复且确认本次商机无效");

        assertEquals(0, validator.validate(reqVO).size());
    }

    @Test
    void wonBusinessDoesNotRequireReason() {
        CrmBusinessUpdateStatusReqVO reqVO = new CrmBusinessUpdateStatusReqVO()
                .setId(1L).setEndStatus(CrmBusinessEndStatusEnum.WIN.getStatus());

        assertEquals(0, validator.validate(reqVO).size());
    }

    @Test
    void endRemarkHasLengthLimit() {
        CrmBusinessUpdateStatusReqVO reqVO = new CrmBusinessUpdateStatusReqVO()
                .setId(1L).setEndStatus(CrmBusinessEndStatusEnum.LOSE.getStatus())
                .setEndRemark("a".repeat(501));

        assertEquals(1, validator.validate(reqVO).size());
    }

    @Test
    void lostBusinessReasonRequiresAtLeastTenCharacters() {
        CrmBusinessUpdateStatusReqVO reqVO = new CrmBusinessUpdateStatusReqVO()
                .setId(1L).setEndStatus(CrmBusinessEndStatusEnum.LOSE.getStatus())
                .setEndRemark("预算取消");

        assertEquals(1, validator.validate(reqVO).size());
    }

    @Test
    void stageAndEndStatusAreMutuallyExclusive() {
        CrmBusinessUpdateStatusReqVO reqVO = new CrmBusinessUpdateStatusReqVO()
                .setId(1L).setStatusId(2L).setStatusRemark("客户需求已确认")
                .setEndStatus(CrmBusinessEndStatusEnum.WIN.getStatus());

        assertEquals(1, validator.validate(reqVO).size());
    }

    @Test
    void stageAdvanceRequiresRemark() {
        CrmBusinessUpdateStatusReqVO reqVO = new CrmBusinessUpdateStatusReqVO()
                .setId(1L).setStatusId(2L).setStatusRemark("   ");

        assertEquals(1, validator.validate(reqVO).size());
    }
}
