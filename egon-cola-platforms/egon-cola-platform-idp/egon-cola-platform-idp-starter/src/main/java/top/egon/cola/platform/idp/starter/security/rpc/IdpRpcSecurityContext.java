package top.egon.cola.platform.idp.starter.security.rpc;

import io.grpc.Context;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.IdpAuthenticationToken;

import java.util.Objects;

/**
 * Carries a verified USER identity through gRPC callbacks and bridges it into Spring Security.
 * The raw token remains in gRPC {@link Context} only for trusted downstream credential relay and
 * is never stored as Spring Security credentials.
 */
public final class IdpRpcSecurityContext {

    private static final Context.Key<IdentityPrincipal> PRINCIPAL =
            Context.key("egon-idp-user-principal");
    private static final Context.Key<String> TOKEN =
            Context.key("egon-idp-user-token");

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
        Objects.requireNonNull(principal, "principal");
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("verified USER token is required");
        }
        return Context.current().withValues(PRINCIPAL, principal, TOKEN, token);
    }

    /**
     * Returns the verified USER token in the current gRPC callback, if present.
     *
     * @return verified token or {@code null}
     */
    public static String currentTokenOrNull() {
        return TOKEN.get();
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
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(callback, "callback");
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext current = SecurityContextHolder.createEmptyContext();
        current.setAuthentication(new IdpAuthenticationToken(principal));
        SecurityContextHolder.setContext(current);
        try {
            callback.run();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
