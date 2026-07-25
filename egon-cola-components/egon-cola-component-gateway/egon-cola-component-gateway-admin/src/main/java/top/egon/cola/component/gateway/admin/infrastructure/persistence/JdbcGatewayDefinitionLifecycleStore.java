package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayDefinitionLifecycleStore;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Repository
public class JdbcGatewayDefinitionLifecycleStore
        implements GatewayDefinitionLifecycleStore {

    private static final String NO_ACTIVE_DEFINITION_SET =
            "__gateway_no_active_definition_set__";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcGatewayDefinitionLifecycleStore(
            NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ReconcileResult reconcile(
            Set<String> activeDefinitionSetIds,
            Instant now) {
        Set<String> activeIds = activeDefinitionSetIds == null
                ? Set.of()
                : Set.copyOf(activeDefinitionSetIds);
        Map<String, Set<String>> applications = new LinkedHashMap<>();
        MapSqlParameterSource scanParameters = new MapSqlParameterSource();
        String scanSql;
        if (activeIds.isEmpty()) {
            scanSql = """
                    SELECT id, application_id
                      FROM gateway_definition_set
                     WHERE status = 'ACTIVE'
                    """;
        } else {
            scanSql = """
                    SELECT id, application_id
                      FROM gateway_definition_set
                     WHERE status = 'ACTIVE'
                        OR id IN (:definitionSetIds)
                    """;
            scanParameters.addValue("definitionSetIds", activeIds);
        }
        jdbc.query(
                scanSql,
                scanParameters,
                (org.springframework.jdbc.core.RowCallbackHandler) result ->
                        collectApplication(
                                applications,
                                activeIds,
                                result.getString("application_id"),
                                result.getString("id")
                        )
        );
        int activatedSets = 0;
        int retiredSets = 0;
        int activatedOperations = 0;
        int offlinedOperations = 0;
        for (Map.Entry<String, Set<String>> application
                : applications.entrySet()) {
            MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue("applicationId", application.getKey())
                    .addValue(
                            "definitionSetIds",
                            application.getValue().isEmpty()
                                    ? Set.of(NO_ACTIVE_DEFINITION_SET)
                                    : application.getValue()
                    )
                    .addValue("now", Timestamp.from(now));
            activatedSets += jdbc.update("""
                    UPDATE gateway_definition_set
                       SET status = 'ACTIVE',
                           activated_at = COALESCE(activated_at, :now),
                           retired_at = NULL
                     WHERE application_id = :applicationId
                       AND id IN (:definitionSetIds)
                       AND status <> 'ACTIVE'
                    """, parameters);
            retiredSets += jdbc.update("""
                    UPDATE gateway_definition_set
                       SET status = 'RETIRED', retired_at = :now
                     WHERE application_id = :applicationId
                       AND status = 'ACTIVE'
                       AND id NOT IN (:definitionSetIds)
                    """, parameters);
            activatedOperations += jdbc.update("""
                    WITH selected AS (
                        SELECT DISTINCT ON (membership.operation_id)
                               membership.operation_id,
                               membership.definition_id,
                               membership.method_identity,
                               membership.provider_service_identity,
                               membership.external_accessible,
                               membership.deprecated
                          FROM gateway_definition_set_operation membership
                          JOIN gateway_definition_set definition_set
                            ON definition_set.id =
                               membership.definition_set_id
                         WHERE definition_set.application_id =
                               :applicationId
                           AND definition_set.id IN (:definitionSetIds)
                         ORDER BY membership.operation_id,
                                  definition_set.received_at DESC
                    )
                    UPDATE gateway_operation operation
                       SET current_definition_id = selected.definition_id,
                           method_identity = selected.method_identity,
                           provider_service_identity =
                               selected.provider_service_identity,
                           external_accessible =
                               selected.external_accessible,
                           lifecycle_status = CASE
                               WHEN selected.deprecated THEN 'DEPRECATED'
                               ELSE 'ACTIVE'
                           END,
                           deprecated_at = CASE
                               WHEN selected.deprecated THEN :now
                               ELSE NULL
                           END,
                           revision = operation.revision + 1,
                           updated_at = :now
                      FROM selected
                     WHERE operation.id = selected.operation_id
                       AND (
                           operation.current_definition_id IS DISTINCT FROM
                               selected.definition_id
                           OR operation.method_identity IS DISTINCT FROM
                               selected.method_identity
                           OR operation.provider_service_identity
                               IS DISTINCT FROM
                               selected.provider_service_identity
                           OR operation.external_accessible IS DISTINCT FROM
                               selected.external_accessible
                           OR operation.lifecycle_status IS DISTINCT FROM
                               CASE WHEN selected.deprecated
                                    THEN 'DEPRECATED' ELSE 'ACTIVE' END
                       )
                    """, parameters);
            offlinedOperations += jdbc.update("""
                    UPDATE gateway_operation operation
                       SET lifecycle_status = 'OFFLINE',
                           revision = operation.revision + 1,
                           updated_at = :now
                     WHERE operation.application_id = :applicationId
                       AND operation.source_type = 'STARTER'
                       AND operation.lifecycle_status <> 'OFFLINE'
                       AND NOT EXISTS (
                           SELECT 1
                             FROM gateway_definition_set_operation membership
                            WHERE membership.operation_id = operation.id
                              AND membership.definition_set_id
                                  IN (:definitionSetIds)
                       )
                    """, parameters);
        }
        return new ReconcileResult(
                activatedSets,
                retiredSets,
                activatedOperations,
                offlinedOperations
        );
    }

    private void collectApplication(
            Map<String, Set<String>> applications,
            Set<String> activeDefinitionSetIds,
            String applicationId,
            String definitionSetId) {
        Set<String> applicationActiveSets = applications.computeIfAbsent(
                applicationId,
                ignored -> new LinkedHashSet<>()
        );
        if (activeDefinitionSetIds.contains(definitionSetId)) {
            applicationActiveSets.add(definitionSetId);
        }
    }
}
