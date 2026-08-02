package top.egon.cola.component.gateway.admin.mcp.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpApprovalStore;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/approvals")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:approve','CAP_*')")
public class McpApprovalController {

    private static final int TOKEN_BYTES = 32;

    private final JdbcMcpApprovalStore approvals;
    private final ObjectMapper objectMapper;
    private final SecureRandom random;
    private final Clock clock;

    @Autowired
    public McpApprovalController(
            JdbcMcpApprovalStore approvals,
            ObjectMapper objectMapper) {
        this(
                approvals,
                objectMapper,
                new SecureRandom(),
                Clock.systemUTC()
        );
    }

    McpApprovalController(
            JdbcMcpApprovalStore approvals,
            ObjectMapper objectMapper,
            SecureRandom random,
            Clock clock) {
        this.approvals = Objects.requireNonNull(approvals, "approvals");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.random = Objects.requireNonNull(random, "random");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalResponse issue(
            @Valid @RequestBody ApprovalRequest request,
            Authentication authentication) {
        ApprovalOwner owner = owner(authentication);
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(request.ttlSeconds());
        String token = token();
        String id = UUID.randomUUID().toString();
        approvals.issue(new JdbcMcpApprovalStore.Approval(
                id,
                McpSecurityDigests.token(token),
                owner.subjectId(),
                owner.tenantId(),
                owner.clientId(),
                request.serverCode(),
                request.toolName(),
                McpSecurityDigests.arguments(
                        objectMapper,
                        request.arguments()
                ),
                issuedAt,
                expiresAt
        ));
        return new ApprovalResponse(id, token, expiresAt);
    }

    private ApprovalOwner owner(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_AUTHENTICATION_REQUIRED"
            );
        }
        if (authentication.getPrincipal()
                instanceof IdentityPrincipal principal) {
            return new ApprovalOwner(
                    principal.subject(),
                    principal.tenantId(),
                    principal.clientId()
            );
        }
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return new ApprovalOwner(
                    jwt.getToken().getSubject(),
                    jwt.getToken().getClaimAsString("tid"),
                    jwt.getToken().getClaimAsString("client_id")
            );
        }
        throw new IllegalStateException(
                "GATEWAY_ADMIN_IDENTITY_PRINCIPAL_REQUIRED"
        );
    }

    private String token() {
        byte[] value = new byte[TOKEN_BYTES];
        random.nextBytes(value);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    public record ApprovalRequest(
            @NotBlank String serverCode,
            @NotBlank String toolName,
            @NotNull Map<String, Object> arguments,
            @Min(1) @Max(300) long ttlSeconds
    ) {
    }

    public record ApprovalResponse(
            String approvalId,
            String approvalToken,
            Instant expiresAt
    ) {

        @Override
        public String toString() {
            return "ApprovalResponse[approvalId=" + approvalId
                    + ", approvalToken=<redacted>, expiresAt="
                    + expiresAt + ']';
        }
    }

    private record ApprovalOwner(
            String subjectId,
            String tenantId,
            String clientId
    ) {

        private ApprovalOwner {
            subjectId = required(subjectId, "subjectId");
            tenantId = required(tenantId, "tenantId");
            clientId = required(clientId, "clientId");
        }

        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
