package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftStore;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

@Repository
public class JdbcGatewayDraftStore implements GatewayDraftStore {

    private final JdbcTemplate jdbc;

    private final ObjectMapper objectMapper;

    public JdbcGatewayDraftStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RouteDraft> routes(String gatewayGroupId) {
        return jdbc.query("""
                SELECT gateway_group_id, route_id, operation_id,
                       route_content::text AS route_content, enabled,
                       updated_at, updated_by
                  FROM gateway_route_draft
                 WHERE gateway_group_id = ?
                 ORDER BY route_id
                """, (result, row) -> new RouteDraft(
                result.getString("gateway_group_id"),
                result.getString("route_id"),
                result.getString("operation_id"),
                map(result.getString("route_content")),
                result.getBoolean("enabled"),
                result.getTimestamp("updated_at").toInstant(),
                result.getString("updated_by")
        ), gatewayGroupId);
    }

    @Override
    public List<PolicyDraft> policies(String gatewayGroupId) {
        return jdbc.query("""
                SELECT gateway_group_id, policy_id, policy_type, policy_scope,
                       policy_content::text AS policy_content, enabled,
                       updated_at, updated_by
                  FROM gateway_policy_draft
                 WHERE gateway_group_id = ?
                 ORDER BY policy_id
                """, (result, row) -> new PolicyDraft(
                result.getString("gateway_group_id"),
                result.getString("policy_id"),
                result.getString("policy_type"),
                result.getString("policy_scope"),
                map(result.getString("policy_content")),
                result.getBoolean("enabled"),
                result.getTimestamp("updated_at").toInstant(),
                result.getString("updated_by")
        ), gatewayGroupId);
    }

    @Override
    public void upsertRoute(RouteDraft route) {
        jdbc.update("""
                INSERT INTO gateway_route_draft(
                    gateway_group_id, route_id, operation_id, route_content,
                    enabled, updated_at, updated_by
                ) VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)
                ON CONFLICT (gateway_group_id, route_id)
                DO UPDATE SET operation_id = EXCLUDED.operation_id,
                              route_content = EXCLUDED.route_content,
                              enabled = EXCLUDED.enabled,
                              updated_at = EXCLUDED.updated_at,
                              updated_by = EXCLUDED.updated_by
                """,
                route.gatewayGroupId(),
                route.routeId(),
                route.operationId(),
                json(route.content()),
                route.enabled(),
                timestamp(route.updatedAt()),
                route.updatedBy()
        );
    }

    @Override
    public void deleteRoute(String gatewayGroupId, String routeId) {
        jdbc.update("""
                DELETE FROM gateway_route_draft
                 WHERE gateway_group_id = ? AND route_id = ?
                """, gatewayGroupId, routeId);
    }

    @Override
    public void upsertPolicy(PolicyDraft policy) {
        jdbc.update("""
                INSERT INTO gateway_policy_draft(
                    gateway_group_id, policy_id, policy_type, policy_scope,
                    policy_content, enabled, updated_at, updated_by
                ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                ON CONFLICT (gateway_group_id, policy_id)
                DO UPDATE SET policy_type = EXCLUDED.policy_type,
                              policy_scope = EXCLUDED.policy_scope,
                              policy_content = EXCLUDED.policy_content,
                              enabled = EXCLUDED.enabled,
                              updated_at = EXCLUDED.updated_at,
                              updated_by = EXCLUDED.updated_by
                """,
                policy.gatewayGroupId(),
                policy.policyId(),
                policy.policyType(),
                policy.policyScope(),
                json(policy.content()),
                policy.enabled(),
                timestamp(policy.updatedAt()),
                policy.updatedBy()
        );
    }

    @Override
    public void deletePolicy(String gatewayGroupId, String policyId) {
        jdbc.update("""
                DELETE FROM gateway_policy_draft
                 WHERE gateway_group_id = ? AND policy_id = ?
                """, gatewayGroupId, policyId);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "draft value cannot be serialized",
                    failure
            );
        }
    }

    private Map<String, Object> map(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored draft value is invalid",
                    failure
            );
        }
    }
}
