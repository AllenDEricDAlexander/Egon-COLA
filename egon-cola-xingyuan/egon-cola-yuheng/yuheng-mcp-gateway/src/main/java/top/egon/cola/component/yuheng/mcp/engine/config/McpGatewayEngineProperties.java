package top.egon.cola.component.yuheng.mcp.engine.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * 中文说明：MCP 进程独立的身份、监听和出站运行参数，不重新映射旧 MCP 能力配置。
 * English summary: Immutable process bootstrap; legacy engine.mcp capability/storage settings remain separate.
 * 用法 / Usage: Bind and validate before constructing listeners, providers or durable state.
 */
@Validated
@ConfigurationProperties(prefix = "egon.cola.component.yuheng.mcp-engine", ignoreUnknownFields = false)
public record McpGatewayEngineProperties(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]{0,63}") String gatewayGroupCode,
        @NotBlank @Size(max = 128) String env,
        @NotBlank @Size(max = 128) String namespace,
        @NotBlank @Size(max = 128) String nodeId,
        @NotBlank @Size(max = 128) String instanceId,
        @NotBlank String dataDirectory,
        @NotNull @Valid @DefaultValue ListenerProperties listener,
        @Min(0) @Max(65535) @DefaultValue("18085") int managementPort,
        @NotNull @Valid @DefaultValue OutboundProperties outbound,
        @NotNull @Valid @DefaultValue ActiveHealthProperties activeHealth
) {

    public McpGatewayEngineProperties {
        gatewayGroupCode = gatewayGroupCode == null ? null : gatewayGroupCode.trim();
        env = env == null ? null : env.trim();
        namespace = namespace == null ? null : namespace.trim();
        nodeId = nodeId == null ? null : nodeId.trim();
        instanceId = instanceId == null ? null : instanceId.trim();
        if (dataDirectory != null && !dataDirectory.isBlank()) {
            Path directory = Path.of(dataDirectory.trim());
            for (Path segment : directory) {
                if ("..".equals(segment.toString())) {
                    throw new IllegalArgumentException("MCP data directory must not traverse parents");
                }
            }
            directory = directory.normalize();
            if (directory.toString().isBlank() || directory.equals(directory.getRoot())) {
                throw new IllegalArgumentException("MCP data directory must name a dedicated directory");
            }
            dataDirectory = directory.toString();
        }
        if (listener != null && managementPort != 0 && listener.port() == managementPort) {
            throw new IllegalArgumentException("MCP data and management ports must differ");
        }
    }

    /**
     * 中文说明：只有一个 MCP 数据监听器；管理 HTTP 使用 Spring Boot 独立端口。
     * English summary: One MCP data listener, separate from the Boot management endpoint.
     */
    public record ListenerProperties(
            @DefaultValue("true") boolean enabled,
            @NotBlank @DefaultValue("0.0.0.0") String host,
            @Min(0) @Max(65535) @DefaultValue("18084") int port,
            @Positive @Max(Integer.MAX_VALUE) @DefaultValue("2097152") long maximumRequestBytes,
            @NotNull @DurationMin(millis = 1) @DefaultValue("10s") Duration drainTimeout,
            @NotNull @Valid @DefaultValue TlsProperties tls
    ) {
    }

    /**
     * 中文说明：复用已有 HTTP 连接池、RPC Channel 和 Operation 调用的边界。
     * English summary: Independent outbound pool, RPC shutdown and operation request limits.
     */
    public record OutboundProperties(
            @Positive @DefaultValue("512") int maxConnections,
            @Positive @DefaultValue("1024") int pendingAcquireMaxCount,
            @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration idleTimeout,
            @NotNull @DurationMin(millis = 1) @DefaultValue("5s") Duration requestTimeout,
            @Positive @DefaultValue("1073741824") long maximumRequestBytes,
            @Positive @DefaultValue("4194304") long maximumResponseBytes,
            @NotNull @DurationMin(millis = 1) @DefaultValue("5s") Duration channelDrainTimeout,
            @NotNull @Valid @DefaultValue TlsProperties rpcTls
    ) {
    }

    /**
     * 中文说明：TLS 配置不能默认为明文，开发明文必须显式开启。
     * English summary: Requires an explicit transport mode; actual file validation uses shared transport security.
     */
    public record TlsProperties(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("false") boolean developmentPlaintext,
            String certificateChainPath,
            String privateKeyPath,
            String trustCertificateCollectionPath,
            @DefaultValue("false") boolean clientCertificateRequired
    ) {
        @AssertTrue(message = "TLS or explicit development plaintext is required")
        public boolean isTransportModeExplicit() {
            return enabled || developmentPlaintext;
        }

        @AssertTrue(message = "enabled TLS requires certificate/key and optional client trust material")
        public boolean isKeyMaterialConfigured() {
            return !enabled || certificateChainPath != null && !certificateChainPath.isBlank()
                    && privateKeyPath != null && !privateKeyPath.isBlank()
                    && (!clientCertificateRequired || trustCertificateCollectionPath != null
                    && !trustCertificateCollectionPath.isBlank());
        }
    }

    /**
     * 中文说明：保持既有 Provider 主动探测默认值，但状态与调度属于 MCP 进程。
     * English summary: Existing active-probe policy knobs with process-local scheduling and health state.
     */
    public record ActiveHealthProperties(
            @DefaultValue("false") boolean enabled,
            @NotNull @DurationMin(millis = 1) @DefaultValue("10s") Duration interval,
            @DecimalMin("0.0") @DecimalMax("1.0") @DefaultValue("0.2") double jitterRatio,
            @NotNull @DurationMin(millis = 1) @DefaultValue("2s") Duration timeout,
            @Positive @DefaultValue("16") int maximumConcurrency,
            @Positive @DefaultValue("2") int failureThreshold,
            @Positive @DefaultValue("2") int successThreshold,
            @NotBlank @DefaultValue("GET") String httpMethod,
            @NotBlank @DefaultValue("/actuator/health") String httpPath,
            @NotEmpty @DefaultValue("200") List<@Min(100) @Max(599) Integer> httpSuccessStatuses,
            @DefaultValue("") String rpcServiceName,
            @DefaultValue("true") boolean rpcConnectFallback
    ) {
        public ActiveHealthProperties {
            httpSuccessStatuses = httpSuccessStatuses == null ? List.of() : List.copyOf(httpSuccessStatuses);
        }
    }
}
