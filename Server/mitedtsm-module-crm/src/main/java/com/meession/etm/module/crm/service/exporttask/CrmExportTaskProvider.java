package com.meession.etm.module.crm.service.exporttask;

import java.util.List;

public interface CrmExportTaskProvider {
    String objectType();
    Integer bizType();
    void validateObjects(List<Long> objectIds);
    ExportFile generate(List<Long> objectIds, Long userId);

    record ExportFile(byte[] content, String fileName, String contentType) {
    }
}
