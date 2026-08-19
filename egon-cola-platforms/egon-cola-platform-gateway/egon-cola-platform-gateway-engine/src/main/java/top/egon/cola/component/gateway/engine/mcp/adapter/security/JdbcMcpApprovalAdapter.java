package top.egon.cola.component.gateway.engine.mcp.adapter.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.mcp.security.McpApprovalPort;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * 中文说明：{@code JdbcMcpApprovalAdapter} 是适配器，位于当前 Gateway 模块的相关包中，负责JdbcMCP审批Adapter相关的职责与边界。
 * English summary: {@code JdbcMcpApprovalAdapter} is a jdbc mcp approval adapter adapter in the current Gateway module; it owns the jdbc mcp approval adapter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class JdbcMcpApprovalAdapter implements McpApprovalPort {

    /**
     * 中文说明：保存 dataSource 对应的状态、依赖或配置值；字段类型为 {@code DataSource}，由 {@code JdbcMcpApprovalAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by data source; its type is {@code DataSource}, and {@code JdbcMcpApprovalAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DataSource dataSource;
    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code JdbcMcpApprovalAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code JdbcMcpApprovalAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code JdbcMcpApprovalAdapter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpApprovalAdapter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param dataSource 参数 dataSource；parameter data source。
     * @param clock 参数 clock；parameter clock。
     */
    public JdbcMcpApprovalAdapter(DataSource dataSource, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 中文说明：执行 consume 操作；该方法是 {@code JdbcMcpApprovalAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the consume operation; this method is the invocation entry point on {@code JdbcMcpApprovalAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalAdapter.consume(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 consume 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<Result> consume(ConsumptionRequest request) {
        Objects.requireNonNull(request, "request");
        return Mono.fromCallable(() -> consumeBlocking(request))
                .onErrorReturn(SQLException.class, Result.UNAVAILABLE)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 中文说明：执行 consumeBlocking 操作；该方法是 {@code JdbcMcpApprovalAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the consume blocking operation; this method is the invocation entry point on {@code JdbcMcpApprovalAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalAdapter.consumeBlocking(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 consumeBlocking 的处理结果；returns the result of the operation.
     */
    private Result consumeBlocking(ConsumptionRequest request)
            throws SQLException {
        Instant now = clock.instant();
        try (Connection connection = dataSource.getConnection()) {
            ApprovalRow row = find(connection, request.tokenDigest());
            if (row == null || !row.matches(request) || !row.validAt(now)) {
                return Result.MISMATCH;
            }
            if ("CONSUMED".equals(row.status())) {
                return Result.CONSUMED;
            }
            if (!"PENDING".equals(row.status())) {
                return Result.MISMATCH;
            }
            if (consume(connection, request, now) == 1) {
                return Result.APPROVED;
            }
            ApprovalRow raced = find(connection, request.tokenDigest());
            return raced != null && "CONSUMED".equals(raced.status())
                    ? Result.CONSUMED
                    : Result.MISMATCH;
        }
    }

    /**
     * 中文说明：执行 find 操作；该方法是 {@code JdbcMcpApprovalAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcMcpApprovalAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalAdapter.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param connection 参数 connection；parameter connection。
     * @param tokenDigest 参数 tokenDigest；parameter token digest。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    private ApprovalRow find(Connection connection, String tokenDigest)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT subject_id, tenant_id, client_id, server_code,
                       tool_name, argument_digest, status, expires_at
                  FROM gateway_mcp_approval
                 WHERE token_digest = ?
                """)) {
            statement.setString(1, tokenDigest);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new ApprovalRow(
                        result.getString("subject_id"),
                        result.getString("tenant_id"),
                        result.getString("client_id"),
                        result.getString("server_code"),
                        result.getString("tool_name"),
                        result.getString("argument_digest"),
                        result.getString("status"),
                        result.getTimestamp("expires_at").toInstant()
                );
            }
        }
    }

    /**
     * 中文说明：执行 consume 操作；该方法是 {@code JdbcMcpApprovalAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the consume operation; this method is the invocation entry point on {@code JdbcMcpApprovalAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalAdapter.consume(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param connection 参数 connection；parameter connection。
     * @param request 参数 请求；parameter request。
     * @param now 参数 now；parameter now。
     * @return 返回 consume 的处理结果；returns the result of the operation.
     */
    private int consume(
            Connection connection,
            ConsumptionRequest request,
            Instant now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE gateway_mcp_approval
                   SET status = 'CONSUMED', consumed_at = ?,
                       revision = revision + 1
                 WHERE token_digest = ?
                   AND subject_id = ?
                   AND tenant_id = ?
                   AND client_id = ?
                   AND server_code = ?
                   AND tool_name = ?
                   AND argument_digest = ?
                   AND status = 'PENDING'
                   AND expires_at > ?
                """)) {
            statement.setTimestamp(1, Timestamp.from(now));
            statement.setString(2, request.tokenDigest());
            statement.setString(3, request.subjectId());
            statement.setString(4, request.tenantId());
            statement.setString(5, request.clientId());
            statement.setString(6, request.serverCode());
            statement.setString(7, request.toolName());
            statement.setString(8, request.argumentDigest());
            statement.setTimestamp(9, Timestamp.from(now));
            return statement.executeUpdate();
        }
    }

    /**
     * 中文说明：{@code ApprovalRow} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审批Row相关的职责与边界。
     * English summary: {@code ApprovalRow} is an immutable data carrier in the current Gateway module; it owns the approval row-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具Name；parameter tool name。
     * @param argumentDigest 参数 argumentDigest；parameter argument digest。
     * @param status 参数 status；parameter status。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     */
    private record ApprovalRow(
            /**
             * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalAdapter.ApprovalRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code JdbcMcpApprovalAdapter.ApprovalRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalAdapter.ApprovalRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalAdapter.ApprovalRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String subjectId,
            /**
             * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalAdapter.ApprovalRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code JdbcMcpApprovalAdapter.ApprovalRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalAdapter.ApprovalRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalAdapter.ApprovalRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tenantId,
            /**
             * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalAdapter.ApprovalRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code JdbcMcpApprovalAdapter.ApprovalRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalAdapter.ApprovalRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalAdapter.ApprovalRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientId,
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalAdapter.ApprovalRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code JdbcMcpApprovalAdapter.ApprovalRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalAdapter.ApprovalRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalAdapter.ApprovalRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 工具Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalAdapter.ApprovalRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tool name; its type is {@code String}, and {@code JdbcMcpApprovalAdapter.ApprovalRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalAdapter.ApprovalRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalAdapter.ApprovalRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String toolName,
            /**
             * 中文说明：保存 argumentDigest 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalAdapter.ApprovalRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by argument digest; its type is {@code String}, and {@code JdbcMcpApprovalAdapter.ApprovalRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalAdapter.ApprovalRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalAdapter.ApprovalRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String argumentDigest,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpApprovalAdapter.ApprovalRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code JdbcMcpApprovalAdapter.ApprovalRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalAdapter.ApprovalRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalAdapter.ApprovalRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String status,
            /**
             * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code JdbcMcpApprovalAdapter.ApprovalRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code JdbcMcpApprovalAdapter.ApprovalRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalAdapter.ApprovalRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalAdapter.ApprovalRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant expiresAt
    ) {

        /**
         * 中文说明：执行 matches 操作；该方法是 {@code JdbcMcpApprovalAdapter.ApprovalRow} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the matches operation; this method is the invocation entry point on {@code JdbcMcpApprovalAdapter.ApprovalRow} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalAdapter.ApprovalRow.matches(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param request 参数 请求；parameter request。
         * @return 返回 matches 的处理结果；returns the result of the operation.
         */
        private boolean matches(ConsumptionRequest request) {
            return subjectId.equals(request.subjectId())
                    && tenantId.equals(request.tenantId())
                    && clientId.equals(request.clientId())
                    && serverCode.equals(request.serverCode())
                    && toolName.equals(request.toolName())
                    && argumentDigest.equals(request.argumentDigest());
        }

        /**
         * 中文说明：执行 validAt 操作；该方法是 {@code JdbcMcpApprovalAdapter.ApprovalRow} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the valid at operation; this method is the invocation entry point on {@code JdbcMcpApprovalAdapter.ApprovalRow} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalAdapter.ApprovalRow.validAt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param now 参数 now；parameter now。
         * @return 返回 validAt 的处理结果；returns the result of the operation.
         */
        private boolean validAt(Instant now) {
            return expiresAt.isAfter(now);
        }
    }
}
