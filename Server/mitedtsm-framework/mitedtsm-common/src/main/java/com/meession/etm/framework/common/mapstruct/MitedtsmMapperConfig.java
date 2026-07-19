package com.meession.etm.framework.common.mapstruct;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * Shared MapStruct policy for intentionally partial domain projections.
 *
 * <p>Conversion behavior remains covered by converter and service tests, while fields that
 * are enriched after the generated mapping do not create compiler warnings.</p>
 */
@MapperConfig(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MitedtsmMapperConfig {
}
