package com.meession.etm.module.crm.controller.admin.statistics.vo.portrait;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.framework.ip.core.enums.AreaTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Schema(description = "管理后台 - CRM 客户画像区域客户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmStatisticsPortraitCustomerPageReqVO extends CrmStatisticsPortraitReqVO {

    @Schema(description = "区域层级：1 国家、2 省份、3 城市", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "3")
    @NotNull(message = "区域层级不能为空")
    @InEnum(value = AreaTypeEnum.class, message = "区域层级必须是 {value}")
    private Integer areaType;

    @Schema(description = "区域编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "110100")
    @NotNull(message = "区域编号不能为空")
    private Integer areaId;

    @Schema(description = "区域及其下级区域编号", hidden = true)
    private List<Integer> areaIds;

}
