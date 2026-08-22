package top.egon.cola.platform.idp.core.resource;

import top.egon.cola.platform.idp.contract.ServiceTokenContext;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * OAuth Client 访问一个 Resource Server 的显式授权。
 *
 * <p>Explicit authorization for an OAuth Client to access one Resource Server.</p>
 *
 * @param clientId          OAuth Client 标识；OAuth Client identifier
 * @param resourceServerId  目标 Resource Server 标识；target Resource Server identifier
 * @param grantType         授权类型；grant type
 * @param tenantId          CLIENT_CREDENTIALS 绑定租户，PLATFORM 时为空；tenant bound to CLIENT_CREDENTIALS, null for PLATFORM
 * @param allowedScopes     IdP 许可的服务 Scope；service scopes permitted by IdP
 * @param status            授权状态；grant status
 * @param version           乐观锁和投影版本；optimistic-lock and projection version
 * @param scopeContext      SERVICE 授权上下文；SERVICE authorization context
 */
public record ClientResourceGrant(
        String clientId,
        String resourceServerId,
        ResourceGrantType grantType,
        String tenantId,
        Set<String> allowedScopes,
        Status status,
        long version,
        ServiceTokenContext scopeContext
) {

    /**
     * 校验用户授权和服务授权的互斥字段。
     *
     * <p>Validates mutually exclusive USER and SERVICE grant facts.</p>
     */
    public ClientResourceGrant {
        clientId = required(clientId, "clientId");
        resourceServerId = required(
                resourceServerId,
                "resourceServerId"
        );
        grantType = Objects.requireNonNull(grantType, "grantType");
        tenantId = optional(tenantId);
        allowedScopes = scopes(allowedScopes);
        status = Objects.requireNonNull(status, "status");
        if (version < 0L) {
            throw new IllegalArgumentException("version must not be negative");
        }
        if (grantType == ResourceGrantType.USER_DELEGATION
                && (tenantId != null
                || !allowedScopes.isEmpty()
                || scopeContext != null)) {
            throw new IllegalArgumentException(
                    "USER_DELEGATION must not contain tenant, scopes or context"
            );
        }
        if (grantType == ResourceGrantType.CLIENT_CREDENTIALS) {
            scopeContext = Objects.requireNonNull(
                    scopeContext,
                    "scopeContext"
            );
            if (allowedScopes.isEmpty()) {
                throw new IllegalArgumentException(
                        "CLIENT_CREDENTIALS requires scopes"
                );
            }
            if (scopeContext == ServiceTokenContext.TENANT
                    && tenantId == null) {
                throw new IllegalArgumentException(
                        "TENANT grant requires tenantId"
                );
            }
            if (scopeContext == ServiceTokenContext.PLATFORM
                    && tenantId != null) {
                throw new IllegalArgumentException(
                        "PLATFORM grant must not contain tenantId"
                );
            }
        }
    }

    /**
     * 兼容旧领域构造签名；服务授权根据 tenant 是否为空推导迁移期 context。
     *
     * <p>Retains the former domain constructor and derives the transitional service context from
     * tenant presence.</p>
     */
    public ClientResourceGrant(
            String clientId,
            String resourceServerId,
            ResourceGrantType grantType,
            String tenantId,
            Set<String> allowedScopes,
            Status status,
            long version
    ) {
        this(
                clientId,
                resourceServerId,
                grantType,
                tenantId,
                allowedScopes,
                status,
                version,
                grantType == ResourceGrantType.CLIENT_CREDENTIALS
                        ? (tenantId == null
                        ? ServiceTokenContext.PLATFORM
                        : ServiceTokenContext.TENANT)
                        : null
        );
    }

    /**
     * 判断授权是否生效。
     *
     * <p>Determines whether the grant is active.</p>
     *
     * @return ACTIVE 时为 {@code true}；{@code true} when ACTIVE
     */
    public boolean active() {
        return status == Status.ACTIVE;
    }

    /**
     * 判断请求 Scope 是否全部在许可集合内。
     *
     * <p>Determines whether every requested scope is allowed.</p>
     *
     * @param requestedScopes 请求 Scope；requested scopes
     * @return 请求集合为非空子集时为 {@code true}；{@code true} when the request is a non-empty subset
     */
    public boolean allows(Set<String> requestedScopes) {
        Set<String> requested = scopes(requestedScopes);
        return !requested.isEmpty() && allowedScopes.containsAll(requested);
    }

    /**
     * 规范化 Scope 集合。
     *
     * <p>Normalizes a scope set.</p>
     *
     * @param values 原始 Scope；raw scopes
     * @return 已排序不可变集合；sorted immutable set
     */
    private static Set<String> scopes(Set<String> values) {
        Objects.requireNonNull(values, "allowedScopes");
        TreeSet<String> normalized = new TreeSet<>();
        for (String value : values) {
            String scope = required(value, "scope");
            if (scope.length() > 256
                    || !scope.matches("[A-Za-z0-9][A-Za-z0-9:._~/-]*")) {
                throw new IllegalArgumentException("scope is invalid");
            }
            if (!normalized.add(scope)) {
                throw new IllegalArgumentException(
                        "allowedScopes contains duplicates"
                );
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    /**
     * 规范化可选文本。
     *
     * <p>Normalizes optional text.</p>
     *
     * @param value 原始文本；raw text
     * @return 规范化文本或 {@code null}；normalized text or {@code null}
     */
    private static String optional(String value) {
        if (value == null) {
            return null;
        }
        if ("*".equals(value)) {
            throw new IllegalArgumentException(
                    "tenantId must not be a wildcard"
            );
        }
        return required(value, "tenantId");
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
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
     * Client Resource Grant 状态。
     *
     * <p>Client Resource Grant status.</p>
     */
    public enum Status {

        /**
         * 授权生效。
         *
         * <p>The grant is active.</p>
         */
        ACTIVE,

        /**
         * 授权禁用。
         *
         * <p>The grant is disabled.</p>
         */
        DISABLED
    }
}
