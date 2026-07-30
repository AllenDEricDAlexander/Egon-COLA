package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityStore;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JdbcGatewayObservabilityStore
        implements GatewayObservabilityStore {

    private final JdbcTemplate jdbc;

    private final NamedParameterJdbcTemplate named;

    public JdbcGatewayObservabilityStore(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        named = new NamedParameterJdbcTemplate(jdbc);
    }

    @Override
    public boolean project(GatewayCallEventV1 event, Instant expiresAt) {
        String provider = providerService(event.routing());
        int inserted = jdbc.update(
                """
                INSERT INTO gateway_call_event_summary (
                    event_id, trace_id, occurred_at, completed_at,
                    duration_ms, protocol, access_zone, env, namespace,
                    gateway_group_id, operation_id, route_id,
                    result_category, gateway_error_code, http_status,
                    grpc_status, engine_node_id, provider_service,
                    attempt_count, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                          ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """,
                event.eventId(),
                event.trace().traceId(),
                timestamp(event.occurredAt()),
                timestamp(event.completedAt()),
                event.result().durationMs(),
                event.request().protocol(),
                event.request().accessZone(),
                event.routing().env(),
                event.routing().namespace(),
                blankToNull(event.routing().gatewayGroupId()),
                blankToNull(event.routing().operationId()),
                blankToNull(event.routing().routeId()),
                event.result().category(),
                blankToNull(event.result().gatewayErrorCode()),
                event.result().httpStatus(),
                blankToNull(event.result().grpcStatus()),
                event.routing().engineNodeId(),
                provider,
                event.attempts().size(),
                Timestamp.from(expiresAt)
        );
        if (inserted == 0) {
            return false;
        }
        Instant bucket = Instant.ofEpochMilli(event.occurredAt())
                .truncatedTo(ChronoUnit.MINUTES);
        jdbc.update(
                """
                INSERT INTO gateway_call_metric_minute (
                    bucket_at, env, namespace, protocol,
                    gateway_group_id, request_count, error_count,
                    duration_total_ms, duration_max_ms
                ) VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?)
                ON CONFLICT (
                    bucket_at, env, namespace, protocol, gateway_group_id
                ) DO UPDATE SET
                    request_count =
                        gateway_call_metric_minute.request_count + 1,
                    error_count =
                        gateway_call_metric_minute.error_count
                            + EXCLUDED.error_count,
                    duration_total_ms =
                        gateway_call_metric_minute.duration_total_ms
                            + EXCLUDED.duration_total_ms,
                    duration_max_ms = GREATEST(
                        gateway_call_metric_minute.duration_max_ms,
                        EXCLUDED.duration_max_ms
                    )
                """,
                Timestamp.from(bucket),
                event.routing().env(),
                event.routing().namespace(),
                event.request().protocol(),
                event.routing().gatewayGroupId(),
                "SUCCESS".equals(event.result().category()) ? 0 : 1,
                event.result().durationMs(),
                event.result().durationMs()
        );
        return true;
    }

    @Override
    public void recordFailure(ConsumeFailure failure) {
        jdbc.update(
                """
                INSERT INTO gateway_call_event_consume_failure (
                    id, topic, partition_no, offset_no, event_id,
                    failure_code, failure_message, payload_sha256,
                    payload_size, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (topic, partition_no, offset_no) DO NOTHING
                """,
                failure.id(),
                failure.topic(),
                failure.partition(),
                failure.offset(),
                blankToNull(failure.eventId()),
                failure.failureCode(),
                failure.failureMessage(),
                failure.payloadSha256(),
                failure.payloadSize(),
                Timestamp.from(failure.occurredAt())
        );
    }

    @Override
    public Page<TraceSummary> traces(TraceQuery query) {
        SqlFilter filter = traceFilter(query);
        long total = named.queryForObject(
                "SELECT count(*) FROM gateway_call_event_summary "
                        + filter.where(),
                filter.parameters(),
                Long.class
        );
        MapSqlParameterSource page = new MapSqlParameterSource(
                filter.parameters().getValues()
        ).addValue("limit", query.size())
                .addValue("offset", (query.page() - 1) * query.size());
        List<TraceSummary> items = named.query(
                """
                SELECT event_id, trace_id, occurred_at, duration_ms,
                       protocol, gateway_group_id, operation_id,
                       result_category, engine_node_id, provider_service
                  FROM gateway_call_event_summary
                """
                        + filter.where()
                        + " ORDER BY occurred_at DESC LIMIT :limit "
                        + "OFFSET :offset",
                page,
                (result, row) -> new TraceSummary(
                        result.getString("event_id"),
                        result.getString("trace_id"),
                        result.getTimestamp("occurred_at").toInstant(),
                        result.getLong("duration_ms"),
                        result.getString("protocol"),
                        result.getString("gateway_group_id"),
                        result.getString("operation_id"),
                        result.getString("result_category"),
                        result.getString("engine_node_id"),
                        result.getString("provider_service")
                )
        );
        return new Page<>(items, query.page(), query.size(), total);
    }

    @Override
    public DashboardSummary dashboard(
            String env,
            String namespace,
            Instant since) {
        long groups = count(
                """
                SELECT count(*) FROM gateway_group
                 WHERE env = ? AND namespace = ? AND deleted = FALSE
                """,
                env,
                namespace
        );
        List<RequestPoint> series = jdbc.query(
                """
                SELECT date_trunc('minute', occurred_at) AS bucket_at,
                       count(*) AS requests,
                       count(*) FILTER (
                           WHERE result_category <> 'SUCCESS'
                       ) AS errors,
                       percentile_cont(0.50) WITHIN GROUP (
                           ORDER BY duration_ms
                       ) AS p50_ms,
                       percentile_cont(0.95) WITHIN GROUP (
                           ORDER BY duration_ms
                       ) AS p95_ms,
                       percentile_cont(0.99) WITHIN GROUP (
                           ORDER BY duration_ms
                       ) AS p99_ms
                  FROM gateway_call_event_summary
                 WHERE env = ? AND namespace = ? AND occurred_at >= ?
                 GROUP BY date_trunc('minute', occurred_at)
                 ORDER BY bucket_at
                """,
                (result, row) -> new RequestPoint(
                        result.getTimestamp("bucket_at").toInstant(),
                        result.getLong("requests"),
                        result.getLong("errors"),
                        Math.round(result.getDouble("p50_ms")),
                        Math.round(result.getDouble("p95_ms")),
                        Math.round(result.getDouble("p99_ms"))
                ),
                env,
                namespace,
                Timestamp.from(since)
        );
        List<ProtocolCall> protocols = jdbc.query(
                """
                SELECT protocol, sum(request_count) AS value
                  FROM gateway_call_metric_minute
                 WHERE env = ? AND namespace = ? AND bucket_at >= ?
                 GROUP BY protocol
                 ORDER BY protocol
                """,
                (result, row) -> new ProtocolCall(
                        result.getString("protocol"),
                        result.getLong("value")
                ),
                env,
                namespace,
                Timestamp.from(since)
        );
        double releaseRate = releaseSuccessRate(env, namespace);
        return new DashboardSummary(
                groups,
                0,
                0,
                0,
                0,
                0,
                releaseRate,
                series,
                protocols,
                series.isEmpty() ? "NO_DATA" : "AVAILABLE"
        );
    }

    @Override
    public Page<AuditSummary> audits(AuditQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("env", query.env())
                .addValue("namespace", query.namespace());
        List<String> clauses = new ArrayList<>(List.of(
                "EXISTS (SELECT 1 FROM gateway_group gg "
                        + "WHERE gg.env = :env AND gg.namespace = :namespace "
                        + "AND (gg.id = a.resource_id "
                        + "OR gg.id = (SELECT r.gateway_group_id "
                        + "FROM gateway_release r "
                        + "WHERE r.id = a.release_id)))"
        ));
        add(clauses, parameters, "a.actor_id", "actorId", query.actorId());
        add(
                clauses,
                parameters,
                "a.resource_id",
                "resourceId",
                query.resourceId()
        );
        add(clauses, parameters, "a.trace_id", "traceId", query.traceId());
        if (query.successful() != null) {
            clauses.add("a.successful = :successful");
            parameters.addValue("successful", query.successful());
        }
        String where = " WHERE " + String.join(" AND ", clauses);
        long total = named.queryForObject(
                "SELECT count(*) FROM gateway_audit_log a" + where,
                parameters,
                Long.class
        );
        parameters.addValue("limit", query.size())
                .addValue("offset", (query.page() - 1) * query.size());
        List<AuditSummary> items = named.query(
                """
                SELECT id, actor_id, actor_type, source, trace_id,
                       resource_type, resource_id, action, before_summary,
                       after_summary, draft_revision, release_id, successful,
                       error_code, occurred_at
                  FROM gateway_audit_log a
                """
                        + where
                        + " ORDER BY occurred_at DESC LIMIT :limit "
                        + "OFFSET :offset",
                parameters,
                (result, row) -> new AuditSummary(
                        result.getString("id"),
                        result.getString("actor_id"),
                        result.getString("actor_type"),
                        result.getString("source"),
                        result.getString("trace_id"),
                        result.getString("resource_type"),
                        result.getString("resource_id"),
                        result.getString("action"),
                        result.getObject("before_summary"),
                        result.getObject("after_summary"),
                        result.getObject("draft_revision", Long.class),
                        result.getString("release_id"),
                        result.getBoolean("successful"),
                        result.getString("error_code"),
                        result.getTimestamp("occurred_at").toInstant()
                )
        );
        return new Page<>(items, query.page(), query.size(), total);
    }

    @Override
    public int deleteExpired(Instant now) {
        return jdbc.update(
                "DELETE FROM gateway_call_event_summary "
                        + "WHERE expires_at < ?",
                Timestamp.from(now)
        );
    }

    private SqlFilter traceFilter(TraceQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("env", query.env())
                .addValue("namespace", query.namespace());
        List<String> clauses = new ArrayList<>(List.of(
                "env = :env",
                "namespace = :namespace"
        ));
        add(clauses, parameters, "trace_id", "traceId", query.traceId());
        add(clauses, parameters, "protocol", "protocol", query.protocol());
        add(
                clauses,
                parameters,
                "result_category",
                "statusCategory",
                query.statusCategory()
        );
        return new SqlFilter(
                " WHERE " + String.join(" AND ", clauses),
                parameters
        );
    }

    private void add(
            List<String> clauses,
            MapSqlParameterSource parameters,
            String column,
            String name,
            String value) {
        if (value != null && !value.isBlank()) {
            clauses.add(column + " = :" + name);
            parameters.addValue(name, value);
        }
    }

    private long count(String sql, Object... parameters) {
        Long value = jdbc.queryForObject(sql, Long.class, parameters);
        return value == null ? 0 : value;
    }

    private double releaseSuccessRate(String env, String namespace) {
        Map<String, Object> counts = jdbc.queryForMap(
                """
                SELECT count(*) AS total,
                       count(*) FILTER (
                           WHERE r.status = 'SUCCEEDED'
                       ) AS succeeded
                  FROM gateway_release r
                  JOIN gateway_group g ON g.id = r.gateway_group_id
                 WHERE g.env = ? AND g.namespace = ?
                """,
                env,
                namespace
        );
        long total = ((Number) counts.get("total")).longValue();
        return total == 0
                ? 0D
                : ((Number) counts.get("succeeded")).doubleValue() / total;
    }

    private String providerService(GatewayCallEventV1.Routing routing) {
        Object value = routing.providerServiceIdentity().get("serviceKey");
        return value == null ? null : String.valueOf(value);
    }

    private Timestamp timestamp(long epochMillis) {
        return Timestamp.from(Instant.ofEpochMilli(epochMillis));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record SqlFilter(
            String where,
            MapSqlParameterSource parameters
    ) {
    }
}
