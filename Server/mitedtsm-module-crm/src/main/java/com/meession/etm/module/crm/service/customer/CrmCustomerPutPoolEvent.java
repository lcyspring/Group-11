package com.meession.etm.module.crm.service.customer;

/** Published inside the ownership transaction and delivered only after commit. */
public record CrmCustomerPutPoolEvent(Long customerId, String customerName, Long previousOwnerUserId,
                                      String source, String reason) {
}
