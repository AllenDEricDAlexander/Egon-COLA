package top.egon.cola.component.gateway.mcp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationRequest;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class McpSecurityGate {

    private final McpAuthorizationPort authorization;
    private final McpApprovalPort approvals;
    private final ObjectMapper objectMapper;

    public McpSecurityGate(
            McpAuthorizationPort authorization,
            McpApprovalPort approvals,
            ObjectMapper objectMapper) {
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
        this.approvals = Objects.requireNonNull(approvals, "approvals");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public Publisher<Void> authorizeToolCall(
            McpRuntimeTool tool,
            IdentityContext identity,
            Map<String, Object> arguments,
            String approvalToken) {
        Objects.requireNonNull(tool, "tool");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(arguments, "arguments");
        McpAuthorizationRequest request = identity.request(
                requiredPermissions(tool)
        );
        return authorize(request)
                .then(approveIfRequired(
                        tool,
                        identity,
                        arguments,
                        approvalToken
                ));
    }

    public Publisher<Void> authorizeResourceRead(
            McpRuntimeResource resource,
            IdentityContext identity) {
        Objects.requireNonNull(resource, "resource");
        return authorize(identity.request(resourcePermissions(
                resource.serverCode(),
                resource.name(),
                resource.requiredPermissions()
        )));
    }

    public Publisher<Void> authorizeResourceRead(
            McpRuntimeResourceTemplate template,
            IdentityContext identity) {
        Objects.requireNonNull(template, "template");
        return authorize(identity.request(resourcePermissions(
                template.serverCode(),
                template.name(),
                template.requiredPermissions()
        )));
    }

    private Mono<Void> authorize(McpAuthorizationRequest request) {
        return Mono.from(authorization.authorize(request))
                .switchIfEmpty(Mono.error(forbidden(
                        "RBAC3_AUTHORIZATION_EMPTY",
                        null
                )))
                .flatMap(decision -> decision.allowed()
                        ? Mono.<Void>empty()
                        : Mono.<Void>error(forbidden(
                        decision.reasonCode(),
                        decision
                )))
                .onErrorMap(
                        failure -> !(failure instanceof McpProtocolException),
                        failure -> forbidden(
                                "RBAC3_AUTHORIZATION_UNAVAILABLE",
                                null
                        )
                );
    }

    private Set<String> resourcePermissions(
            String serverCode,
            String name,
            Set<String> declared) {
        TreeSet<String> permissions = new TreeSet<>(declared);
        permissions.add("mcp:" + serverCode
                + ":resource:" + name + ":read");
        return Set.copyOf(permissions);
    }

    private Mono<Void> approveIfRequired(
            McpRuntimeTool tool,
            IdentityContext identity,
            Map<String, Object> arguments,
            String approvalToken) {
        if (!requiresApproval(tool)) {
            return Mono.empty();
        }
        if (approvalToken == null || approvalToken.isBlank()) {
            return Mono.error(new McpProtocolException(
                    McpErrorCode.MCP_APPROVAL_REQUIRED,
                    "MCP approval is required for this tool"
            ));
        }
        McpApprovalPort.ConsumptionRequest request =
                new McpApprovalPort.ConsumptionRequest(
                        McpSecurityDigests.token(approvalToken),
                        identity.subjectId(),
                        identity.tenantId(),
                        identity.clientId(),
                        tool.serverCode(),
                        tool.name(),
                        McpSecurityDigests.arguments(objectMapper, arguments)
                );
        return Mono.from(approvals.consume(request))
                .switchIfEmpty(Mono.just(McpApprovalPort.Result.UNAVAILABLE))
                .flatMap(result -> switch (result) {
                    case APPROVED -> Mono.empty();
                    case MISMATCH -> Mono.error(new McpProtocolException(
                            McpErrorCode.MCP_APPROVAL_MISMATCH,
                            "MCP approval does not match this request"
                    ));
                    case CONSUMED -> Mono.error(new McpProtocolException(
                            McpErrorCode.MCP_APPROVAL_CONSUMED,
                            "MCP approval was already consumed"
                    ));
                    case UNAVAILABLE -> Mono.error(forbidden(
                            "MCP_APPROVAL_UNAVAILABLE",
                            null
                    ));
                });
    }

    private Set<String> requiredPermissions(McpRuntimeTool tool) {
        TreeSet<String> permissions = new TreeSet<>(
                tool.requiredPermissions()
        );
        permissions.add("mcp:" + tool.serverCode()
                + ":tool:" + tool.name() + ":call");
        return Set.copyOf(permissions);
    }

    private boolean requiresApproval(McpRuntimeTool tool) {
        String risk = tool.riskLevel().toUpperCase(Locale.ROOT);
        return "HIGH".equals(risk) || "CRITICAL".equals(risk);
    }

    private McpProtocolException forbidden(
            String reasonCode,
            McpAuthorizationPort.Decision decision) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("reasonCode", reasonCode);
        if (decision != null) {
            data.put("authVersion", decision.authVersion());
            data.put("contextVersion", decision.contextVersion());
            data.put("policyVersion", decision.policyVersion());
        }
        return new McpProtocolException(
                McpErrorCode.MCP_FORBIDDEN,
                "MCP authorization was denied",
                Map.copyOf(data)
        );
    }

    public record IdentityContext(
            String issuer,
            String subjectId,
            String tenantId,
            String sessionId,
            String clientId,
            String tokenId,
            long tokenVersion,
            Set<String> audience,
            Instant issuedAt,
            Instant expiresAt,
            long minimumAuthVersion,
            long minimumContextVersion,
            long minimumPolicyVersion
    ) {

        public IdentityContext {
            issuer = required(issuer, "issuer");
            subjectId = required(subjectId, "subjectId");
            tenantId = required(tenantId, "tenantId");
            sessionId = required(sessionId, "sessionId");
            clientId = required(clientId, "clientId");
            tokenId = required(tokenId, "tokenId");
            nonNegative(tokenVersion, "tokenVersion");
            audience = Set.copyOf(Objects.requireNonNull(
                    audience,
                    "audience"
            ));
            if (audience.isEmpty()) {
                throw new IllegalArgumentException(
                        "audience must not be empty"
                );
            }
            issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
            expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
            if (!expiresAt.isAfter(issuedAt)) {
                throw new IllegalArgumentException(
                        "expiresAt must be after issuedAt"
                );
            }
            nonNegative(minimumAuthVersion, "minimumAuthVersion");
            nonNegative(minimumContextVersion, "minimumContextVersion");
            nonNegative(minimumPolicyVersion, "minimumPolicyVersion");
        }

        private McpAuthorizationRequest request(Set<String> permissions) {
            return new McpAuthorizationRequest(
                    issuer,
                    subjectId,
                    tenantId,
                    sessionId,
                    clientId,
                    tokenId,
                    tokenVersion,
                    audience,
                    issuedAt,
                    expiresAt,
                    permissions,
                    minimumAuthVersion,
                    minimumContextVersion,
                    minimumPolicyVersion
            );
        }

        public static IdentityContext from(
                Map<String, Object> attributes) {
            Objects.requireNonNull(attributes, "attributes");
            return new IdentityContext(
                    text(attributes, "identity.issuer", "idp.issuer"),
                    text(attributes, "identity.subject", "callerId"),
                    text(attributes, "identity.tenant-id", "tenantId"),
                    text(
                            attributes,
                            "identity.session-id",
                            "idp.session-id"
                    ),
                    text(
                            attributes,
                            "identity.client-id",
                            "idp.client-id"
                    ),
                    text(
                            attributes,
                            "identity.token-id",
                            "idp.token-id"
                    ),
                    number(
                            attributes,
                            "identity.token-version",
                            "idp.token-version"
                    ),
                    strings(
                            attributes,
                            "identity.audience",
                            "idp.audience"
                    ),
                    instant(
                            attributes,
                            "identity.issued-at",
                            "idp.issued-at"
                    ),
                    instant(
                            attributes,
                            "identity.expires-at",
                            "idp.expires-at"
                    ),
                    optionalNumber(attributes, "rbac3.auth-version"),
                    optionalNumber(attributes, "rbac3.context-version"),
                    optionalNumber(attributes, "rbac3.policy-version")
            );
        }

        private static String text(
                Map<String, Object> attributes,
                String... keys) {
            Object value = first(attributes, keys);
            if (!(value instanceof String text) || text.isBlank()) {
                throw new IllegalArgumentException(
                        keys[0] + " is required"
                );
            }
            return text.trim();
        }

        private static long number(
                Map<String, Object> attributes,
                String... keys) {
            Object value = first(attributes, keys);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                return Long.parseLong(text.trim());
            }
            throw new IllegalArgumentException(keys[0] + " is required");
        }

        private static long optionalNumber(
                Map<String, Object> attributes,
                String key) {
            Object value = attributes.get(key);
            if (value == null) {
                return 0L;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                return Long.parseLong(text.trim());
            }
            throw new IllegalArgumentException(key + " must be a number");
        }

        private static Instant instant(
                Map<String, Object> attributes,
                String... keys) {
            Object value = first(attributes, keys);
            if (value instanceof Instant instant) {
                return instant;
            }
            if (value instanceof String text && !text.isBlank()) {
                return Instant.parse(text.trim());
            }
            throw new IllegalArgumentException(keys[0] + " is required");
        }

        private static Set<String> strings(
                Map<String, Object> attributes,
                String... keys) {
            Object value = first(attributes, keys);
            TreeSet<String> values = new TreeSet<>();
            if (value instanceof Collection<?> collection) {
                collection.forEach(item -> {
                    if (!(item instanceof String text) || text.isBlank()) {
                        throw new IllegalArgumentException(
                                keys[0] + " must contain strings"
                        );
                    }
                    values.add(text.trim());
                });
            } else if (value instanceof String text && !text.isBlank()) {
                for (String item : text.split(",")) {
                    if (!item.isBlank()) {
                        values.add(item.trim());
                    }
                }
            }
            if (values.isEmpty()) {
                throw new IllegalArgumentException(
                        keys[0] + " must not be empty"
                );
            }
            return Set.copyOf(values);
        }

        private static Object first(
                Map<String, Object> attributes,
                String... keys) {
            for (String key : keys) {
                Object value = attributes.get(key);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        private static void nonNegative(long value, String field) {
            if (value < 0L) {
                throw new IllegalArgumentException(
                        field + " must not be negative"
                );
            }
        }

        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
