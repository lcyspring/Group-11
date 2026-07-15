package com.meession.etm.module.crm.controller.admin.clue.vo.publicpool;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 公共线索分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmCluePublicPageReqVO extends PageParam {

    @Schema(description = "线索名称")
    private String name;
    @Schema(description = "手机号")
    private String mobile;
    @Schema(description = "行业")
    private Integer industryId;
    @Schema(description = "等级")
    private Integer level;
    @Schema(description = "来源")
    private Integer source;
}
