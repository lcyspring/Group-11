package com.meession.etm.module.bpm.controller.admin.oa.vo;
import jakarta.validation.constraints.*; import lombok.Data; import java.util.Map;
@Data public class BpmOAWorkRequestCreateReqVO { @NotBlank @Size(max=200) private String title; @NotBlank @Size(min=5,max=10000) private String content; @NotNull @Min(1) @Max(3) private Integer urgency = 1; private Map<String, java.util.List<Long>> startUserSelectAssignees; }
