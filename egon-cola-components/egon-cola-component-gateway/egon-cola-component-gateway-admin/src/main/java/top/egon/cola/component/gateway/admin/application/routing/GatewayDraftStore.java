package top.egon.cola.component.gateway.admin.application.routing;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface GatewayDraftStore {

    List<RouteDraft> routes(String gatewayGroupId);

    List<PolicyDraft> policies(String gatewayGroupId);

    void upsertRoute(RouteDraft route);

    void deleteRoute(String gatewayGroupId, String routeId);

    void upsertPolicy(PolicyDraft policy);

    void deletePolicy(String gatewayGroupId, String policyId);

    record RouteDraft(
            String gatewayGroupId,
            String routeId,
            String operationId,
            Map<String, Object> content,
            boolean enabled,
            Instant updatedAt,
            String updatedBy
    ) {
    }

    record PolicyDraft(
            String gatewayGroupId,
            String policyId,
            String policyType,
            String policyScope,
            Map<String, Object> content,
            boolean enabled,
            Instant updatedAt,
            String updatedBy
    ) {
    }
}
