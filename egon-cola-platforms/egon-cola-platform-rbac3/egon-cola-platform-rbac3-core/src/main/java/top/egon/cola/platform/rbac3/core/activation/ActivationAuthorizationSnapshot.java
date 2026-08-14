package top.egon.cola.platform.rbac3.core.activation;

import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.core.decision.DataScopeMerger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public record ActivationAuthorizationSnapshot(
        Set<String> effectiveRoleIds,
        Set<String> permissionCodes,
        Map<String, DataScopeMerger.NormalizedDataScope> dataScopes,
        Map<String, FieldAccessLevel> fieldPolicies,
        Set<String> resourceCodes,
        String landingRouteCode,
        long authVersion,
        long policyVersion,
        String checksum
) {

    public ActivationAuthorizationSnapshot {
        effectiveRoleIds = Collections.unmodifiableSet(new TreeSet<>(effectiveRoleIds));
        permissionCodes = Collections.unmodifiableSet(new TreeSet<>(permissionCodes));
        dataScopes = Collections.unmodifiableMap(new TreeMap<>(dataScopes));
        fieldPolicies = Collections.unmodifiableMap(new TreeMap<>(fieldPolicies));
        resourceCodes = Collections.unmodifiableSet(new TreeSet<>(resourceCodes));
    }
}
