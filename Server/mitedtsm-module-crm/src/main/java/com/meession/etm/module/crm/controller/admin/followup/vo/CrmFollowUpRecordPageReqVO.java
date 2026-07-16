package com.meession.etm.module.crm.controller.admin.followup.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 跟进记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmFollowUpRecordPageReqVO extends PageParam {

    @Schema(description = "数据类型", example = "2")
    @NotNull
    @Min(1)
    private Integer bizType;

    @Schema(description = "数据编号", example = "5564")
    @NotNull
    @Min(1)
    private Long bizId;

}
