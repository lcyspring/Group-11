package com.meession.etm.module.system.api.mail.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class MailTemplateReadinessRespDTO {
    private boolean templateExists;
    private boolean templateEnabled;
    private boolean accountConfigured;
    private List<String> missingParams = new ArrayList<>();
}
