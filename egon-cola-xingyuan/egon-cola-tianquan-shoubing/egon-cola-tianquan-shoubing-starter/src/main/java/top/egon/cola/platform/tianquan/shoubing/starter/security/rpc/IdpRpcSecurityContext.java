package top.egon.cola.platform.tianquan.shoubing.starter.security.rpc;

import io.grpc.Context;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.context.request.RequestContextHolder;
import org.slf4j.MDC;

import java.util.Objects;
import java.util.Set;

/**
 * Carries one verified USER or SERVICE identity through gRPC callbacks. Only USER tokens are
 * retained for relay; Spring credentials stay empty and each callback restores prior thread state.
 */
public final class IdpRpcSecurityContext {

    private static final Context.Key<IdpPrincipal> PRINCIPAL =
            Context.key("egon-tianquan-shoubing-user-principal");
    private static final Context.Key<String> TOKEN =
            Context.key("egon-tianquan-shoubing-user-token");

    private static final Context.Key<TokenAuditProofBO> PROOF = Context.key("egon-tianquan-shoubing-audit-proof");
    private static final Context.Key<Set<String>> SCOPES = Context.key("egon-tianquan-shoubing-oauth-scopes");
    private static final Context.Key<IdpAuthorizationObservationContextBO> OBSERVATION = Context.key("egon-tianquan-shoubing-authorization-observation");

    private IdpRpcSecurityContext() {
    }

    /**
     * Creates a child gRPC context containing the verified USER principal and token.
     *
     * @param principal verified USER principal
     * @param token verified raw USER access token
     * @return child context scoped to the current RPC
     */
    public static Context with(IdentityPrincipal principal, String token) {
        return with(principal, token, null);
    }

    public static Context with(IdentityPrincipal principal, String token, TokenAuditProofBO proof) {
        return with(principal, token, proof, null, Set.of());
    }

    public static Context with(IdpPrincipal principal, String token, TokenAuditProofBO proof,
            IdpAuthorizationObservationContextBO marker) {
        return with(principal, token, proof, marker, principal instanceof ServiceIdentityPrincipal service ? service.scopes() : Set.of());
    }

    public static Context with(IdpPrincipal principal, String token, TokenAuditProofBO proof,
            IdpAuthorizationObservationContextBO marker, Set<String> scopes) {
        var verified = new AccessTokenVerification.Valid<>(Objects.requireNonNull(principal, "principal"), scopes, proof);
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("verified access token is required");
        }
        // Suppress a parent USER relay slot on a nested SERVICE call.
        return Context.current().withValues(PRINCIPAL, principal, TOKEN, principal instanceof IdentityPrincipal ? token : null)
                .withValues(PROOF, proof, SCOPES, verified.scopes(), OBSERVATION, marker);
    }

    /**
     * Returns the verified USER token in the current gRPC callback, if present.
     *
     * @return verified token or {@code null}
     */
    public static TokenAuditProofBO currentProofOrNull() { return PROOF.get(); }
    public static IdpPrincipal currentPrincipalOrNull() { return PRINCIPAL.get(); }
    public static IdpAuthorizationObservationContextBO currentAuthorizationObservationContext() { return OBSERVATION.get(); }

    public static String currentTokenOrNull() {
        return PRINCIPAL.get() instanceof IdentityPrincipal ? TOKEN.get() : null;
    }

    /**
     * Runs a callback with an identity-only Spring Security context and restores the exact
     * previous context afterward.
     *
     * @param principal verified USER principal
     * @param callback downstream callback
     */
    public static void runWithSpringSecurity(
            IdentityPrincipal principal,
            Runnable callback
    ) {
        runWithSpringSecurity(principal, currentProofOrNull(), callback);
    }

    public static void runWithSpringSecurity(IdentityPrincipal principal, TokenAuditProofBO proof, Runnable callback) {
        Set<String> scopes = Objects.equals(PRINCIPAL.get(), principal) && Objects.equals(PROOF.get(), proof) && SCOPES.get() != null
                ? SCOPES.get() : Set.of();
        runWithSpringSecurity(principal, scopes, proof, callback);
    }

    public static void runWithSpringSecurity(IdpPrincipal principal, Set<String> scopes, TokenAuditProofBO proof, Runnable callback) {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(callback, "callback");
        var checked = new AccessTokenVerification.Valid<>(principal, scopes, proof);
        SecurityContext previous = SecurityContextHolder.getContext();
        var previousMdc = MDC.getCopyOfContextMap();
        var previousAttributes = RequestContextHolder.getRequestAttributes();
        SecurityContext current = SecurityContextHolder.createEmptyContext();
        var authentication = new IdpAuthenticationToken(principal, checked.scopes().stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope)).toList());
        authentication.setDetails(proof);
        current.setAuthentication(authentication);
        try {
            SecurityContextHolder.setContext(current);
            RequestContextHolder.resetRequestAttributes();
            if (principal.tenantId() == null) { MDC.remove("tenantId"); } else { MDC.put("tenantId", principal.tenantId()); }
            MDC.put("userId", principal instanceof ServiceIdentityPrincipal service ? service.clientId() : principal.subject());
            MDC.put("egon.auth.actor-kind", principal instanceof ServiceIdentityPrincipal ? "SERVICE" : "USER");
            callback.run();
        } finally {
            SecurityContextHolder.setContext(previous);
            RequestContextHolder.setRequestAttributes(previousAttributes);
            MDC.clear();
            if (previousMdc != null) { MDC.setContextMap(previousMdc); }
        }
    }
}
