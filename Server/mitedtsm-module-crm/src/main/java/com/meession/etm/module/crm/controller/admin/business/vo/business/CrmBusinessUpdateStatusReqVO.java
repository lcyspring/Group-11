package com.meession.etm.module.crm.controller.admin.business.vo.business;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.crm.enums.business.CrmBusinessEndStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - CRM 商机更新状态 Request VO")
@Data
public class CrmBusinessUpdateStatusReqVO {

    @Schema(description = "商机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "32129")
    @NotNull(message = "商机编号不能为空")
    private Long id;

    @Schema(description = "状态编号", example = "1")
    private Long statusId;

    @Schema(description = "结束状态", example = "1")
    @InEnum(value = CrmBusinessEndStatusEnum.class)
    private Integer endStatus;

    @Schema(description = "阶段推进说明；推进普通阶段时必填", example = "客户已确认需求范围")
    @Size(max = 500, message = "阶段推进说明不能超过 500 个字符")
    private String statusRemark;

    @Schema(description = "结束原因；输单或无效时必填", example = "客户预算取消")
    @Size(max = 500, message = "结束原因不能超过 500 个字符")
    private String endRemark;

    @AssertTrue(message = "变更状态不正确")
    public boolean isStatusValid() {
        return (statusId != null) ^ (endStatus != null);
    }

    @AssertTrue(message = "输单或无效时结束原因至少需要 10 个字符")
    public boolean isEndRemarkValid() {
        if (endStatus == null || CrmBusinessEndStatusEnum.WIN.getStatus().equals(endStatus)) {
            return true;
        }
        return endRemark != null && endRemark.trim().length() >= 10;
    }

    @AssertTrue(message = "推进普通阶段时必须填写阶段推进说明")
    public boolean isStatusRemarkValid() {
        return statusId == null || statusRemark != null && !statusRemark.trim().isEmpty();
    }

}
