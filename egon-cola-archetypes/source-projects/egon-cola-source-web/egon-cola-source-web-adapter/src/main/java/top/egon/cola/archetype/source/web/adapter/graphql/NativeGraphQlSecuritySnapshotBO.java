package top.egon.cola.archetype.source.web.adapter.graphql;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeAuthorizationContext;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;

/** Immutable server capture; Servlet request lifecycle and shared audit marker remain separate framework state. */
public record NativeGraphQlSecuritySnapshotBO(
        @NotNull @Valid RuntimeAuthorizationContext authorizationContext,
        @NotNull Set<@NotBlank String> verifiedScopes,
        @NotNull @Valid TokenAuditProofBO auditProof,
        @NotNull TraceContext traceContext,
        @NotNull @Valid OrganizationRequestContext businessContext) {
    public NativeGraphQlSecuritySnapshotBO {
        Objects.requireNonNull(authorizationContext, "authorizationContext");
        verifiedScopes = Set.copyOf(Objects.requireNonNull(verifiedScopes, "verifiedScopes"));
        Objects.requireNonNull(auditProof, "auditProof");
        Objects.requireNonNull(traceContext, "traceContext");
        Objects.requireNonNull(businessContext, "businessContext");
        if (!authorizationContext.identity().subject().equals(businessContext.actorId())
                || businessContext.actorId().isBlank() || businessContext.actorId().length() > 128
                || !businessContext.actorRoles().isEmpty()) {
            throw new IllegalArgumentException("NATIVE_GRAPHQL_IDENTITY_MISMATCH");
        }
    }
}
