package com.meession.etm.module.crm.framework.permission;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Owner users whose CRM objects are readable through the current user's organization data scope.
 * Organization scope never grants write, owner, transfer, delete, or export permission.
 */
public record CrmOwnerReadScope(boolean all, Set<Long> ownerUserIds) {

    public CrmOwnerReadScope {
        ownerUserIds = ownerUserIds == null
                ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(ownerUserIds));
    }

    public boolean allows(Long ownerUserId) {
        return all || ownerUserIds.contains(ownerUserId);
    }

}
