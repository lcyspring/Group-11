package com.meession.etm.module.bpm.controller.admin.oa.vo;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class BpmOADocumentSaveReqVO { private Long id; private Long parentId; @NotBlank @Size(max=200) private String name; @Size(max=2000) private String description; @NotNull @Min(0) @Max(1) private Integer visibility = 0; }
