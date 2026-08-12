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

/**
 * 中文说明：{@code JdbcGatewayObservabilityStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责Jdbc网关可观测性存储相关的职责与边界。
 * English summary: {@code JdbcGatewayObservabilityStore} is a jdbc gateway observability store store in the current Gateway module; it owns the jdbc gateway observability store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public class JdbcGatewayObservabilityStore
        implements GatewayObservabilityStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcGatewayObservabilityStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcGatewayObservabilityStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayObservabilityStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayObservabilityStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 named 对应的状态、依赖或配置值；字段类型为 {@code NamedParameterJdbcTemplate}，由 {@code JdbcGatewayObservabilityStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by named; its type is {@code NamedParameterJdbcTemplate}, and {@code JdbcGatewayObservabilityStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayObservabilityStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayObservabilityStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final NamedParameterJdbcTemplate named;

    /**
     * 中文说明：创建 {@code JdbcGatewayObservabilityStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcGatewayObservabilityStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     */
    public JdbcGatewayObservabilityStore(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        named = new NamedParameterJdbcTemplate(jdbc);
    }

    /**
     * 中文说明：执行 project 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the project operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.project(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     * @return 返回 project 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 recordFailure 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the record failure operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.recordFailure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     */
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

    /**
     * 中文说明：执行 traces 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the traces operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.traces(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 traces 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 dashboard 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dashboard operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.dashboard(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param since 参数 since；parameter since。
     * @return 返回 dashboard 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 audits 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audits operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.audits(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 audits 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 deleteExpired 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete expired operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.deleteExpired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 deleteExpired 的处理结果；returns the result of the operation.
     */
    @Override
    public int deleteExpired(Instant now) {
        return jdbc.update(
                "DELETE FROM gateway_call_event_summary "
                        + "WHERE expires_at < ?",
                Timestamp.from(now)
        );
    }

    /**
     * 中文说明：执行 trace过滤器 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace filter operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.traceFilter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 trace过滤器 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 add 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the add operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.add(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param clauses 参数 clauses；parameter clauses。
     * @param parameters 参数 parameters；parameter parameters。
     * @param column 参数 column；parameter column。
     * @param name 参数 name；parameter name。
     * @param value 参数 值；parameter value。
     */
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

    /**
     * 中文说明：执行 count 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the count operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.count(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sql 参数 sql；parameter sql。
     * @param parameters 参数 parameters；parameter parameters。
     * @return 返回 count 的处理结果；returns the result of the operation.
     */
    private long count(String sql, Object... parameters) {
        Long value = jdbc.queryForObject(sql, Long.class, parameters);
        return value == null ? 0 : value;
    }

    /**
     * 中文说明：执行 发布SuccessRate 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the release success rate operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.releaseSuccessRate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 发布SuccessRate 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 提供方服务 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the provider service operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.providerService(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param routing 参数 routing；parameter routing。
     * @return 返回 提供方服务 的处理结果；returns the result of the operation.
     */
    private String providerService(GatewayCallEventV1.Routing routing) {
        Object value = routing.providerServiceIdentity().get("serviceKey");
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 中文说明：执行 timestamp 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the timestamp operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.timestamp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param epochMillis 参数 epochMillis；parameter epoch millis。
     * @return 返回 timestamp 的处理结果；returns the result of the operation.
     */
    private Timestamp timestamp(long epochMillis) {
        return Timestamp.from(Instant.ofEpochMilli(epochMillis));
    }

    /**
     * 中文说明：执行 blankToNull 操作；该方法是 {@code JdbcGatewayObservabilityStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the blank to null operation; this method is the invocation entry point on {@code JdbcGatewayObservabilityStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayObservabilityStore.blankToNull(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 blankToNull 的处理结果；returns the result of the operation.
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 中文说明：{@code SqlFilter} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Sql过滤器相关的职责与边界。
     * English summary: {@code SqlFilter} is an immutable data carrier in the current Gateway module; it owns the sql filter-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param where 参数 where；parameter where。
     * @param parameters 参数 parameters；parameter parameters。
     */
    private record SqlFilter(
            /**
             * 中文说明：保存 where 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayObservabilityStore.SqlFilter} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by where; its type is {@code String}, and {@code JdbcGatewayObservabilityStore.SqlFilter} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcGatewayObservabilityStore.SqlFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayObservabilityStore.SqlFilter}; do not couple callers to its representation when the owning type exposes an API.
             */
            String where,
            /**
             * 中文说明：保存 parameters 对应的状态、依赖或配置值；字段类型为 {@code MapSqlParameterSource}，由 {@code JdbcGatewayObservabilityStore.SqlFilter} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by parameters; its type is {@code MapSqlParameterSource}, and {@code JdbcGatewayObservabilityStore.SqlFilter} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcGatewayObservabilityStore.SqlFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayObservabilityStore.SqlFilter}; do not couple callers to its representation when the owning type exposes an API.
             */
            MapSqlParameterSource parameters
    ) {
    }
}
