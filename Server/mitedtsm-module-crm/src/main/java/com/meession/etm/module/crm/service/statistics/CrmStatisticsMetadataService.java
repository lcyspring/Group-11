package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.module.crm.controller.admin.statistics.vo.metadata.CrmStatisticsMetadataCatalogRespVO;

public interface CrmStatisticsMetadataService {

    CrmStatisticsMetadataCatalogRespVO getCatalog(String scope);
}
