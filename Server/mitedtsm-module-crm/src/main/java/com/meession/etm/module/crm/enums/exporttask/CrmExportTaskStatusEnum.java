package com.meession.etm.module.crm.enums.exporttask;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmExportTaskStatusEnum {
    QUEUED(10), RUNNING(20), SUCCESS(30), FAILED(40), EXPIRED(50);

    private final int status;
}
