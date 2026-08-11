package top.egon.cola.platform.idp.admin.support.rbac3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import top.egon.cola.platform.idp.core.port.UserResourceAccessAuthorizationPort;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

/**
 * 调用 RBAC3 内部接口完成 USER Resource 入口权限判定。
 *
 * <p>Calls the RBAC3 internal endpoint to decide USER Resource entry permission.</p>
 *
 * <p>适配器只接收最小决策与版本栅栏，不把角色、权限列表或数据规则带回 IdP。</p>
 *
 * <p>The adapter accepts only a minimal decision and version fences; it does not bring roles,
 * permission lists, or data rules back into IdP.</p>
 */
public final class HttpUserResourceAccessAuthorizationAdapter
        implements UserResourceAccessAuthorizationPort {

    /** 安全记录内部调用失败，不输出 Token 或用户请求；safe internal-call logger without tokens or user payloads. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HttpUserResourceAccessAuthorizationAdapter.class
    );

    /** RBAC3 决策路径；RBAC3 decision path. */
    private static final String DECISION_PATH =
            "/internal/v1/authorization/resource-access-decisions";

    /** HTTP 客户端；HTTP client. */
    private final RestClient restClient;

    /** RBAC3 基础地址；RBAC3 base URL. */
    private final String baseUrl;

    /** 按目标租户提供 IdP 服务身份请求头；provides IdP service headers by target tenant. */
    private final Function<String, String> authorizationHeader;

    /**
     * 创建 RBAC3 USER Resource 决策适配器。
     *
     * <p>Creates the RBAC3 USER Resource decision adapter.</p>
     *
     * @param restClient HTTP 客户端；HTTP client
     * @param baseUrl RBAC3 基础地址；RBAC3 base URL
     * @param authorizationHeader 按租户提供 IdP 服务身份请求头；IdP service header by tenant
     */
    public HttpUserResourceAccessAuthorizationAdapter(
            RestClient restClient,
            String baseUrl,
            Function<String, String> authorizationHeader
    ) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.baseUrl = validBaseUrl(baseUrl);
        this.authorizationHeader = Objects.requireNonNull(
                authorizationHeader,
                "authorizationHeader"
        );
    }

    /**
     * 请求一个最小 USER Resource 入口决策。
     *
     * <p>Requests one minimal USER Resource entry decision.</p>
     *
     * @param request USER Resource 入口请求；USER Resource entry request
     * @return 允许或拒绝结论及版本栅栏；allow or deny result with version fences
     * @throws AccessUnavailableException RBAC3 不可用或响应无法验证时抛出；when RBAC3 is unavailable or its response cannot be validated
     */
    @Override
    public AccessDecision decide(AccessRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            DecisionEnvelope envelope = restClient.post()
                    .uri(baseUrl + DECISION_PATH)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            serviceAuthorization(request.tenantId())
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DecisionRequest(
                            required(request.identitySub(), "identitySub"),
                            required(request.tenantId(), "tenantId"),
                            required(request.sessionId(), "sessionId"),
                            required(
                                    request.rbacApplicationCode(),
                                    "rbacApplicationCode"
                            ),
                            required(
                                    request.entryPermissionCode(),
                                    "entryPermissionCode"
                            )
                    ))
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            (sent, received) -> {
                                throw new IllegalStateException(
                                        "RBAC3 decision HTTP "
                                                + received.getStatusCode().value()
                                );
                            }
                    )
                    .body(DecisionEnvelope.class);
            return toDomain(envelope);
        } catch (AccessUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "RBAC3 Resource access decision call failed: {}: {}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            throw unavailable();
        }
    }

    /**
     * 校验并映射 RBAC3 决策。
     *
     * <p>Validates and maps an RBAC3 decision.</p>
     *
     * @param envelope RBAC3 统一响应；RBAC3 response envelope
     * @return 领域决策；domain decision
     */
    private AccessDecision toDomain(DecisionEnvelope envelope) {
        if (envelope == null || envelope.data() == null
                || envelope.meta() == null) {
            throw unavailable();
        }
        try {
            required(envelope.meta().requestId(), "requestId");
            required(envelope.meta().traceId(), "traceId");
            Objects.requireNonNull(envelope.meta().timestamp(), "timestamp");
            DecisionResponse response = envelope.data();
            Decision decision = Decision.valueOf(required(
                    response.decision(),
                    "decision"
            ));
            String reason = required(response.reasonCode(), "reasonCode");
            Objects.requireNonNull(response.decidedAt(), "decidedAt");
            return new AccessDecision(
                    decision,
                    reason,
                    nonNegative(response.authVersion(), "authVersion"),
                    nonNegative(response.sessionVersion(), "sessionVersion"),
                    nonNegative(response.policyVersion(), "policyVersion")
            );
        } catch (IllegalArgumentException exception) {
            throw unavailable();
        }
    }

    /**
     * 获取并校验服务身份请求头。
     *
     * <p>Obtains and validates the service-identity request header.</p>
     *
     * @param tenantId 精确目标租户；exact target tenant
     * @return 可安全发送的 Authorization 请求头；safe Authorization header
     */
    private String serviceAuthorization(String tenantId) {
        String value = authorizationHeader.apply(required(
                tenantId,
                "tenantId"
        ));
        if (value == null
                || value.isBlank()
                || !value.equals(value.trim())
                || value.contains("\r")
                || value.contains("\n")) {
            throw unavailable();
        }
        return value;
    }

    /**
     * 校验 RBAC3 基础地址并移除末尾斜杠。
     *
     * <p>Validates the RBAC3 base URL and removes its trailing slash.</p>
     *
     * @param value 原始基础地址；raw base URL
     * @return 规范化地址；normalized URL
     */
    private static String validBaseUrl(String value) {
        URI uri = URI.create(required(value, "baseUrl"));
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("invalid RBAC3 base URL");
        }
        String normalized = uri.toString();
        return normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    /**
     * 校验非负版本字段。
     *
     * <p>Validates a non-negative version field.</p>
     *
     * @param value 版本值；version value
     * @param field 字段名；field name
     * @return 已校验版本；validated version
     */
    private static long nonNegative(Long value, String field) {
        if (value == null || value < 0L) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验文本；text to validate
     * @param field 字段名；field name
     * @return 已校验文本；validated text
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 创建不暴露底层传输细节的不可用异常。
     *
     * <p>Creates an unavailable exception without exposing transport details.</p>
     *
     * @return RBAC3 决策不可用异常；RBAC3 decision unavailable exception
     */
    private static AccessUnavailableException unavailable() {
        return new AccessUnavailableException(
                "RBAC3 Resource access decision is unavailable"
        );
    }

    /**
     * RBAC3 决策请求体。
     *
     * <p>RBAC3 decision request body.</p>
     *
     * @param identitySub 用户身份；user identity
     * @param tid 租户；tenant
     * @param sid 身份会话；identity session
     * @param rbacApplicationCode RBAC3 应用；RBAC3 application
     * @param entryPermissionCode 入口权限；entry permission
     */
    private record DecisionRequest(
            String identitySub,
            String tid,
            String sid,
            String rbacApplicationCode,
            String entryPermissionCode
    ) {
    }

    /**
     * RBAC3 统一响应包。
     *
     * <p>RBAC3 response envelope.</p>
     *
     * @param data 最小决策数据；minimal decision data
     * @param meta 统一响应元数据；response metadata
     */
    private record DecisionEnvelope(
            DecisionResponse data,
            ResponseMeta meta
    ) {
    }

    /**
     * RBAC3 统一响应元数据。
     *
     * <p>RBAC3 response metadata.</p>
     *
     * @param requestId 请求标识；request identifier
     * @param traceId 链路标识；trace identifier
     * @param timestamp 响应时间；response timestamp
     */
    private record ResponseMeta(
            String requestId,
            String traceId,
            Instant timestamp
    ) {
    }

    /**
     * RBAC3 最小决策响应。
     *
     * <p>RBAC3 minimal decision response.</p>
     *
     * @param decision 允许或拒绝；allow or deny
     * @param reasonCode 稳定原因码；stable reason code
     * @param authVersion 授权版本；authorization version
     * @param sessionVersion 会话版本；session version
     * @param policyVersion 策略版本；policy version
     * @param decidedAt 判定时间；decision time
     */
    private record DecisionResponse(
            String decision,
            String reasonCode,
            Long authVersion,
            Long sessionVersion,
            Long policyVersion,
            Instant decidedAt
    ) {
    }
}
